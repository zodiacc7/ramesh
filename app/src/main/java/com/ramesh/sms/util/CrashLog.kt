package com.ramesh.sms.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the last uncaught exception on disk so a "sometimes it crashes" bug can actually be looked
 * at.
 *
 * Nothing here swallows the crash: the previous handler is always called afterwards, so the system
 * still records and reports it. The file is a *copy* for the user to share from Settings, which is
 * the only way to see a stack trace from a release build without a connected computer.
 */
object CrashLog {

    private const val TAG = "CrashLog"
    private const val FILE_NAME = "last_crash.txt"

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, thread, error) }
                .onFailure { Log.w(TAG, "Could not store the crash log", it) }
            // Hand the crash back to the default handler: the app must still die the way Android
            // expects, and Play Console must still receive it.
            previous?.uncaughtException(thread, error)
        }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { writer ->
            PrintWriter(writer).use { error.printStackTrace(it) }
        }.toString()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val text = buildString {
            appendLine("Ramesh crash")
            appendLine("time: $stamp")
            appendLine("thread: ${thread.name}")
            appendLine("version: ${versionName(context)}")
            appendLine("android: ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.MODEL})")
            appendLine()
            append(trace)
        }
        File(context.filesDir, FILE_NAME).writeText(text)
    }

    /** The stored trace, or null when the last run ended cleanly. */
    fun read(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        return runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }

    private fun versionName(context: Context): String =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
}
