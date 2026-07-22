package com.auroramind.meditation

import android.app.Activity

/**
 * Billing has been removed. Power of Mind is 100% free (ad-supported) — there
 * are no in-app purchases or subscriptions, so the app no longer bundles the
 * Google Play Billing Library at all. This is what clears Play's "update to
 * Billing Library 8.0.0+" requirement: with no billing library in the app,
 * there is nothing to version.
 *
 * This lightweight no-op stub preserves the original public surface so the
 * existing call sites in MainActivity / DashboardActivity keep compiling with
 * no changes. Every method intentionally does nothing, and [onPremiumChanged]
 * is never invoked — the app renders its free tier by default (ads on, all
 * content unlocked elsewhere). Safe to delete along with its call sites in a
 * later cleanup.
 */
class BillingManager(
    @Suppress("UNUSED_PARAMETER") activity: Activity,
    @Suppress("UNUSED_PARAMETER") onPremiumChanged: (Boolean) -> Unit,
) {
    /** No-op: there is nothing to query without a billing client. */
    fun queryPurchases() {}

    /** No-op: nothing to purchase — the whole app is free. */
    fun purchaseUnlock() {}

    /** No-op: no subscriptions. Signals "unavailable" so any caller falls back. */
    fun purchaseAnnual(onUnavailable: () -> Unit = {}) { onUnavailable() }

    /** No-op: no subscriptions. */
    fun purchaseMonthly() {}

    /** No-op: no billing client to tear down. */
    fun destroy() {}
}
