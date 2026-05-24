// Analytics helper for logging events
package com.soundpad.sleep

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

object AnalyticsHelper {
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (analytics == null) {
            analytics = FirebaseAnalytics.getInstance(context)
        }
    }

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        analytics?.logEvent(name) {
            params.forEach { (k, v) ->
                when (v) {
                    is String -> param(k, v)
                    is Long -> param(k, v)
                    is Double -> param(k, v)
                    is Int -> param(k, v.toLong())
                    is Boolean -> param(k, if (v) 1L else 0L)
                }
            }
        }
    }
}
