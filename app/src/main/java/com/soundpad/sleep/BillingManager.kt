package com.soundpad.sleep

import android.app.Activity
import com.android.billingclient.api.*
import kotlinx.coroutines.*

/**
 * Wraps Google Play Billing Library 6.x.
 *
 * Products to create in Google Play Console:
 *   In-app product  → soundpad_pro         ($3.99 one-time)  "SoundPad Pro"
 *   Subscription    → soundpad_ultimate    ($1.99/month)     "SoundPad Ultimate"
 *
 * Both are treated as "premium" in this app — either unlocks everything.
 */
class BillingManager(
    private val activity: Activity,
    private val onPremiumChanged: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val SKU_PRO      = "soundpad_pro"
        const val SKU_ULTIMATE = "soundpad_ultimate"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val client = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Retry handled by the Play Store — no manual retry needed
            }
        })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PurchasesUpdatedListener
    // ─────────────────────────────────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { acknowledge(it) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query existing purchases on resume
    // ─────────────────────────────────────────────────────────────────────────

    fun queryPurchases() {
        scope.launch {
            var hasPremium = false

            // Check one-time purchases
            val inappResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                hasPremium = hasPremium || inappResult.purchasesList.any { p ->
                    p.products.contains(SKU_PRO) &&
                    p.purchaseState == Purchase.PurchaseState.PURCHASED
                }
            }

            // Check subscriptions
            val subsResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                hasPremium = hasPremium || subsResult.purchasesList.any { p ->
                    p.products.contains(SKU_ULTIMATE) &&
                    p.purchaseState == Purchase.PurchaseState.PURCHASED
                }
            }

            onPremiumChanged(hasPremium)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Launch purchase flows
    // ─────────────────────────────────────────────────────────────────────────

    /** One-time purchase: SoundPad Pro ($3.99) */
    fun purchasePro() = launchPurchase(SKU_PRO, BillingClient.ProductType.INAPP)

    /** Monthly subscription: SoundPad Ultimate ($1.99/month) */
    fun purchaseUltimate() = launchPurchase(SKU_ULTIMATE, BillingClient.ProductType.SUBS)

    private fun launchPurchase(productId: String, productType: String) {
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

            val productDetails = result.productDetailsList?.firstOrNull() ?: return@launch

            val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

            // Subscriptions need an offer token
            if (productType == BillingClient.ProductType.SUBS) {
                val offerToken = productDetails.subscriptionOfferDetails
                    ?.firstOrNull()?.offerToken ?: return@launch
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
    // Acknowledge purchase (required — else Google refunds after 3 days)
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
            }
        }
    }

    fun destroy() {
        scope.cancel()
        client.endConnection()
    }
}
