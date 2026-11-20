package com.steevsapps.idledaddy.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.preferences.PrefsManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Catches uncaught exceptions and dumps a crash report to the Downloads folder
 * before handing off to the previous default handler (which kills the process).
 */
class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Read at crash time, so toggling the setting needs no restart.
        val enabled = try {
            PrefsManager.crashLogger()
        } catch (_: Throwable) {
            false
        }
        val heapDump = if (enabled && isOutOfMemory(throwable)) heapDumpFile() else null
        if (enabled) {
            try {
                saveCrashReport(throwable, heapDump)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash report", e)
            }
        }
        if (heapDump != null) {
            // After the report, so a failed or slow dump can't cost us the stack trace.
            try {
                Debug.dumpHprofData(heapDump.absolutePath)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to dump heap", t)
            }
        }
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable)
        } else {
            exitProcess(1)
        }
    }

    /** True if [throwable] or anything in its cause chain is an [OutOfMemoryError]. */
    private fun isOutOfMemory(throwable: Throwable): Boolean =
        generateSequence(throwable) { it.cause }.take(MAX_CAUSE_DEPTH).any { it is OutOfMemoryError }

    /**
     * Where the heap dump goes: the app's external files dir (`Android/data/<package>/files`),
     * reachable over USB/adb. One fixed name, so a new dump replaces the old one.
     */
    private fun heapDumpFile(): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, HEAP_DUMP_NAME)

    private fun saveCrashReport(throwable: Throwable, heapDump: File?) {
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val report = buildString {
            appendLine("Time: ${TIMESTAMP_FORMAT.format(Date())}")
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            if (heapDump != null) {
                appendLine("Heap dump: ${heapDump.absolutePath}")
            }
            appendLine()
            append(stackTrace)
        }
        val fileName = "IdleDaddy_crash_${FILE_NAME_FORMAT.format(Date())}.txt"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(fileName, report)
        } else {
            saveViaLegacyFile(fileName, report)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(fileName: String, report: String) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { it.write(report.toByteArray()) }
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyFile(fileName: String, report: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) return
        File(downloadsDir, fileName).writeText(report)
    }

    companion object {
        private val TAG: String = CrashHandler::class.java.simpleName
        private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        private val FILE_NAME_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        private const val HEAP_DUMP_NAME = "IdleDaddy_oom.hprof"
        private const val MAX_CAUSE_DEPTH = 16

        fun install(context: Context) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current is CrashHandler) {
                return
            }
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context.applicationContext, current)
            )
        }
    }
}