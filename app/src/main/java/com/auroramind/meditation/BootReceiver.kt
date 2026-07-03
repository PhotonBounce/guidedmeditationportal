package com.auroramind.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the meditation alarm and daily reminder after a reboot.
 * This is the only exported receiver (BOOT_COMPLETED requires it) and it
 * strictly checks the action, so a spoofed intent can at worst re-arm
 * already-saved schedules — it can never trigger a ring.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        AlarmScheduler.reschedule(context)
        ReminderScheduler.reschedule(context)
    }
}
