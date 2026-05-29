package com.soundpad.sleep

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Wraps Play's In-App Review API.
 *
 * The native in-app review modal lifts completion rate roughly 3–5× over a
 * market:// intent that punts users out of the app. Use the in-app modal
 * for normal "are you enjoying SoundPad?" prompts; only fall back to the
 * Play Store deep link if the API errors.
 *
 * Google rate-limits these to ~1 per user per several months — calling more
 * often silently no-ops, which is fine.
 */
object InAppReviewHelper {

    private const val TAG = "SoundPadReview"

    /** Request the native review modal. Falls back to Play Store on error. */
    fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val info = task.result
                manager.launchReviewFlow(activity, info).addOnCompleteListener {
                    // Whether the user actually rated is opaque by design;
                    // we just continue. Google tracks completion server-side.
                }
            } else {
                Log.w(TAG, "Review flow unavailable, falling back to Play Store")
                openPlayStoreListing(activity)
            }
        }
    }

    private fun openPlayStoreListing(activity: Activity) {
        runCatching {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${activity.packageName.removeSuffix(".debug")}"))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            )
        }
    }
}
