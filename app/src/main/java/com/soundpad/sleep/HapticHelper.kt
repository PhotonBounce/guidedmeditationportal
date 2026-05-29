package com.soundpad.sleep

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Tasteful haptic feedback. Uses Android 10+ predefined effects when
 * available (these map to the user's preferred haptic profile) and falls
 * back to short vibration patterns otherwise.
 *
 * Levels:
 *   tick   — every button tap
 *   click  — play / stop / timer
 *   thud   — purchase, reward granted (heavier confirmation)
 */
class HapticHelper(context: Context) {

    private val prefs = PrefsManager(context)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun tick()  = vibrate(VibrationEffect.EFFECT_TICK,        fallbackMs = 8)
    fun click() = vibrate(VibrationEffect.EFFECT_CLICK,       fallbackMs = 18)
    fun thud()  = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, fallbackMs = 35)

    private fun vibrate(effectId: Int, fallbackMs: Long) {
        if (!prefs.isHapticsEnabled()) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(effectId))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(fallbackMs)
            }
        } catch (_: Throwable) {
            // Some OEM ROMs throw on predefined effects — ignore, haptics are optional.
        }
    }
}
