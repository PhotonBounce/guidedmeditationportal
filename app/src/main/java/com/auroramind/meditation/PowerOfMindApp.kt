package com.auroramind.meditation

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Application entry point. Its one job is to install [CrashReporter] as early as
 * possible so that *any* uncaught exception — on any thread, in any Activity — is
 * persisted to disk before the process dies. On the next launch [SplashActivity]
 * reads that file and shows the stack trace, so a crash can be shared without adb.
 */
class PowerOfMindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}

/**
 * Lightweight on-device crash recorder.
 *
 * Chains the platform's default uncaught-exception handler: we write the trace to
 * `filesDir/last_crash.txt`, log it, then delegate to the original handler so the
 * OS crash dialog and logcat behave exactly as before. Nothing here can itself
 * throw into the crashing thread — every step is wrapped.
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"
    private const val TAG = "PowerOfMindCrash"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                PrintWriter(sw).use { throwable.printStackTrace(it) }
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val report = buildString {
                    append("Power of Mind — crash report\n")
                    append("time:   ").append(stamp).append('\n')
                    append("thread: ").append(thread.name).append('\n')
                    append("app:    ").append(BuildConfig.APPLICATION_ID)
                        .append(" v").append(BuildConfig.VERSION_NAME).append('\n')
                    append("device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                        .append(" (Android ").append(Build.VERSION.RELEASE)
                        .append(", API ").append(Build.VERSION.SDK_INT).append(")\n\n")
                    append(sw.toString())
                }
                appContext.openFileOutput(FILE, Context.MODE_PRIVATE).use {
                    it.write(report.toByteArray())
                }
                Log.e(TAG, report)
            }
            // Always let the platform handler finish the job (crash dialog + kill).
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** The last persisted crash report, or null if there isn't one. */
    fun read(context: Context): String? = runCatching {
        val f = File(context.filesDir, FILE)
        if (f.exists() && f.length() > 0) f.readText() else null
    }.getOrNull()

    /** Forget the last crash once it has been seen / shared. */
    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
    }
}
