package com.auroramind.meditation

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*

/**
 * Wraps Google Play Billing Library 8.x.
 *
 * Products to create in Google Play Console (Monetize → Products):
 *   In-app product  → meditation_portal_unlock   ($2.00 one-time)   "Meditation Portal — Full Unlock"
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
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    @Volatile private var connected = false
    @Volatile private var connecting = false

    private val client = BillingClient.newBuilder(activity)
        .setListener(this)
        // Billing 8: the parameterless enablePendingPurchases() was removed
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        connect()
    }

    private fun connect() {
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connected = true
                    queryPurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                connecting = false
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
        if (!connected) {
            // A failed initial setup would otherwise wedge billing for the
            // activity's lifetime — retry the connection; a successful setup
            // re-runs this query automatically.
            connect()
            return
        }
        scope.launch {
            // One-time INAPP unlock
            val inappResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            if (inappResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                // Query FAILED (Play outage, service disconnect, …) — keep the
                // cached premium state rather than revoking a paying user.
                Log.w(TAG, "Purchase query failed: ${inappResult.billingResult.debugMessage}")
                return@launch
            }

            val hasPremium = inappResult.purchasesList.any { p ->
                p.products.contains(SKU_UNLOCK) &&
                p.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            // Auto-acknowledge any unacked purchases (otherwise Google refunds after 3 days!)
            inappResult.purchasesList.forEach { if (!it.isAcknowledged) acknowledge(it) }

            onPremiumChanged(hasPremium)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Launch purchase flows
    // ─────────────────────────────────────────────────────────────────────────

    /** One-time unlock purchase: removes ads, opens the full library ($2.00) */
    fun purchaseUnlock() = launchPurchase(SKU_UNLOCK, BillingClient.ProductType.INAPP)

    private fun launchPurchase(productId: String, productType: String) {
        if (!connected) {
            Log.w(TAG, "Billing not connected — ignoring purchase")
            return
        }
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType)
                        .build()
                )
            )
            .build()

        // Billing 8: the response listener now receives a QueryProductDetailsResult
        // (productDetailsList + unfetchedProductList) instead of a plain list.
        client.queryProductDetailsAsync(queryParams) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Product details query failed: ${result.debugMessage}")
                return@queryProductDetailsAsync
            }

            val productDetails = detailsResult.productDetailsList.firstOrNull()
            if (productDetails == null) {
                Log.w(TAG, "No product details for $productId — is it Active in Play Console?")
                return@queryProductDetailsAsync
            }

            val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

            // Subscriptions need an offer token
            if (productType == BillingClient.ProductType.SUBS) {
                val offerToken = productDetails.subscriptionOfferDetails
                    ?.firstOrNull()?.offerToken
                if (offerToken == null) {
                    Log.w(TAG, "No subscription offer for $productId")
                    return@queryProductDetailsAsync
                }
                paramsBuilder.setOfferToken(offerToken)
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                .build()

            // Billing callbacks arrive on a binder thread — the purchase flow
            // must launch from the main thread.
            scope.launch {
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
