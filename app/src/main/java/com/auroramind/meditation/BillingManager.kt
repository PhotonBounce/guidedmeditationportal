package com.auroramind.meditation

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*

/**
 * Wraps Google Play Billing Library 6.x.
 *
 * Products to create in Google Play Console (Monetize → Products):
 *   In-app product  → meditation_portal_unlock   ($0.49 one-time)   "Meditation Portal — Full Unlock"
 *
 * The single unlock purchase removes ads and opens the full guided
 * meditation library + Spirit + alarm forever.
 *
 * IMPORTANT: the product must be ACTIVE in Play Console before billing
 * returns product details. Until then, queryProductDetails returns an
 * empty list and the purchase flow silently no-ops — that's expected.
 */
class BillingManager(
    private val activity: Activity,
    private val onPremiumChanged: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val TAG        = "PortalBilling"
        const val SKU_UNLOCK = "meditation_portal_unlock"

        // Quit-habit + affirmations subscriptions.
        // Create these in Play Console → Monetize → Subscriptions before they
        // resolve; until ACTIVE, queryProductDetails returns empty and the
        // purchase flow no-ops (same behaviour as the one-time unlock below).
        const val SKU_ANNUAL  = "pom_annual"
        const val SKU_MONTHLY = "pom_monthly"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    @Volatile private var connected = false

    private val client = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        connect()
    }

    private fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connected = true
                    queryPurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                connected = false
                // Reconnect with simple back-off (Play Store handles its own retry)
                scope.launch {
                    delay(2000)
                    if (!connected) runCatching { connect() }
                }
            }
        })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PurchasesUpdatedListener
    // ─────────────────────────────────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { acknowledge(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
                Log.w(TAG, "Purchase update error ${result.responseCode}: ${result.debugMessage}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query existing purchases on resume
    // ─────────────────────────────────────────────────────────────────────────

    fun queryPurchases() {
        if (!connected) return
        scope.launch {
            var hasPremium = false

            // One-time INAPP unlock
            val inappResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                hasPremium = hasPremium || inappResult.purchasesList.any { p ->
                    p.products.contains(SKU_UNLOCK) &&
                    p.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                // Auto-acknowledge any unacked purchases (otherwise Google refunds after 3 days!)
                inappResult.purchasesList.forEach { if (!it.isAcknowledged) acknowledge(it) }
            }

            // Active SUBS subscription (annual or monthly)
            val subsResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                hasPremium = hasPremium || subsResult.purchasesList.any { p ->
                    (p.products.contains(SKU_ANNUAL) || p.products.contains(SKU_MONTHLY)) &&
                    p.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                subsResult.purchasesList.forEach { if (!it.isAcknowledged) acknowledge(it) }
            }

            onPremiumChanged(hasPremium)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Launch purchase flows
    // ─────────────────────────────────────────────────────────────────────────

    /** One-time unlock purchase: removes ads, opens the full library ($0.49) */
    fun purchaseUnlock() = launchPurchase(SKU_UNLOCK, BillingClient.ProductType.INAPP)

    /** Annual subscription — the primary plan offered on the post-quiz paywall. */
    fun purchaseAnnual() = launchPurchase(SKU_ANNUAL, BillingClient.ProductType.SUBS)

    /** Monthly subscription — the secondary plan on the paywall. */
    fun purchaseMonthly() = launchPurchase(SKU_MONTHLY, BillingClient.ProductType.SUBS)

    private fun launchPurchase(productId: String, productType: String) {
        if (!connected) {
            Log.w(TAG, "Billing not connected — ignoring purchase")
            return
        }
        scope.launch {
            val result = client.queryProductDetails(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(productType)
                                .build()
                        )
                    )
                    .build()
            )

            val productDetails = result.productDetailsList?.firstOrNull()
            if (productDetails == null) {
                Log.w(TAG, "No product details for $productId — is it Active in Play Console?")
                return@launch
            }

            val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

            // Subscriptions need an offer token
            if (productType == BillingClient.ProductType.SUBS) {
                val offerToken = productDetails.subscriptionOfferDetails
                    ?.firstOrNull()?.offerToken
                if (offerToken == null) {
                    Log.w(TAG, "No subscription offer for $productId")
                    return@launch
                }
                paramsBuilder.setOfferToken(offerToken)
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                .build()

            withContext(Dispatchers.Main) {
                client.launchBillingFlow(activity, flowParams)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Acknowledge purchase (REQUIRED — else Google refunds after 3 days)
    // ─────────────────────────────────────────────────────────────────────────

    private fun acknowledge(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) {
            onPremiumChanged(true)
            return
        }
        scope.launch {
            val result = client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            )
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                onPremiumChanged(true)
            } else {
                Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
            }
        }
    }

    fun destroy() {
        scope.cancel()
        runCatching { client.endConnection() }
    }
}
