package com.auroramind.meditation

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Home-screen widget — shows the user's current streak and today's bite-sized
 * technique, with a one-tap "Meditate now" button that opens the app. A daily
 * re-engagement surface (the single best place to keep streaks alive).
 *
 * Updated hourly by the system, and on demand via [refreshAll] (e.g. after a
 * session is recorded).
 */
class StreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) renderWidget(context, mgr, id)
    }

    companion object {
        /** Re-render every placed widget — call after stats change. */
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, StreakWidgetProvider::class.java))
            for (id in ids) renderWidget(context, mgr, id)
        }

        private fun renderWidget(context: Context, mgr: AppWidgetManager, id: Int) {
            val stats = StatsManager(context)
            val tech = MicroTechniques.forGoal(PrefsManager(context).getGoal()).firstOrNull() ?: MicroTechniques.today()

            val views = RemoteViews(context.packageName, R.layout.widget_streak)
            views.setTextViewText(R.id.widgetStreak, "🔥 ${stats.currentStreak()}")
            views.setTextViewText(R.id.widgetTechnique, "${tech.emoji} ${tech.title}")

            // Tap anywhere or the button → open the app via the splash
            val open = Intent(context, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pi)
            views.setOnClickPendingIntent(R.id.widgetCta, pi)

            mgr.updateAppWidget(id, views)
        }
    }
}
