package com.steevsapps.idledaddy.listeners

import android.util.Log
import `in`.dragonbra.javasteam.util.log.LogListener
import kotlin.math.min

/**
 * LogListener that prints to Android Logcat
 */
class AndroidLogListener : LogListener {

    override fun onLog(clazz: Class<*>, message: String?, throwable: Throwable) {
        var threadName = Thread.currentThread().name
        threadName = threadName.substring(0, min(10, threadName.length))
        val className = clazz.getName()
        if (message == null) {
            Log.i(TAG, String.format("[%10s] %s", threadName, className), throwable)
        } else {
            Log.i(TAG, String.format("[%10s] %s - %s", threadName, className, message), throwable)
        }
    }

    override fun onError(clazz: Class<*>, message: String?, throwable: Throwable) {
        var threadName = Thread.currentThread().name
        threadName = threadName.substring(0, min(10, threadName.length))
        val className = clazz.getName()
        if (message == null) {
            Log.e(TAG, String.format("[%10s] %s", threadName, className), throwable)
        } else {
            Log.e(TAG, String.format("[%10s] %s - %s", threadName, className, message), throwable)
        }
    }

    companion object {
        private const val TAG = "JavaSteam"
    }
}
