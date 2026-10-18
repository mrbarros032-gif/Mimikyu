package com.lin0721.linmusic.core.log

import android.content.Context
import android.os.Build
import com.lin0721.linmusic.BuildConfig

object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val TAG = "CrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {

            val deviceInfo = """
                ================ FATAL CRASH ================
                App Version : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                OS Version  : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                Device      : ${Build.BRAND} ${Build.MODEL}
                Manufacturer: ${Build.MANUFACTURER}
                Hardware    : ${Build.HARDWARE}
                Thread Name : ${thread.name}
                =============================================
            """.trimIndent()

            AppLogger.e(TAG, "$deviceInfo\nCrash Stack Trace:", throwable)

            // IO  of  Channel
            Thread.sleep(800)
        } catch (e: Exception) {
            // ，
        } finally {
            // ，
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
