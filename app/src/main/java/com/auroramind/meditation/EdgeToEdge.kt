package com.auroramind.meditation

import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Edge-to-edge helpers for Android 15 (SDK 35), where every activity draws
 * under the system bars whether it opts in or not, and the old window
 * attributes (statusBarColor / navigationBarColor / windowLightStatusBar /
 * FLAG_FULLSCREEN) plus Window.setDecorFitsSystemWindows are deprecated.
 *
 * Every screen calls [goEdgeToEdge] before setContentView. Screens whose
 * layout is not designed to run under the bars then inset their content with
 * [padSystemBars]; immersive canvas screens call [hideSystemBars] instead.
 */

/**
 * Opts the window into edge-to-edge on every API level with transparent
 * system bars. The app is visually dark on every screen regardless of the
 * device theme, so both bars always get light icons (SystemBarStyle.dark) —
 * the same look the old windowLightStatusBar=false attribute produced.
 */
fun ComponentActivity.goEdgeToEdge() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
}

/**
 * Insets this view by the system-bar and display-cutout sizes so its content
 * clears the bars while whatever is behind it (e.g. the AuraView backdrop)
 * still paints the full screen. Padding present in the layout is preserved —
 * insets are added on top of it. With [ime] the bottom inset also tracks the
 * on-screen keyboard, which is required on API 30+ where adjustResize is
 * ignored for edge-to-edge windows.
 */
fun View.padSystemBars(top: Boolean = true, bottom: Boolean = true, ime: Boolean = false) {
    val l0 = paddingLeft
    val t0 = paddingTop
    val r0 = paddingRight
    val b0 = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val bottomInset = if (ime) {
            maxOf(bars.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom)
        } else bars.bottom
        v.setPadding(
            l0 + bars.left,
            if (top) t0 + bars.top else t0,
            r0 + bars.right,
            if (bottom) b0 + bottomInset else b0,
        )
        insets
    }
}

/**
 * Fully hides the status and navigation bars for immersive canvas screens
 * (splash, breathing, player, milestones); a swipe from the edge reveals
 * them transiently. Modern replacement for android:windowFullscreen.
 */
fun ComponentActivity.hideSystemBars() {
    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
