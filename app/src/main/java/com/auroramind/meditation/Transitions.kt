package com.auroramind.meditation

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent

/**
 * Starts [intent] with the app's standard cross-fade. Replaces the
 * startActivity + overridePendingTransition pair — overridePendingTransition
 * is deprecated on API 34+, while ActivityOptions.makeCustomAnimation works
 * unchanged on every supported API level.
 */
fun Activity.startActivityFading(intent: Intent) {
    startActivity(
        intent,
        ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out).toBundle()
    )
}
