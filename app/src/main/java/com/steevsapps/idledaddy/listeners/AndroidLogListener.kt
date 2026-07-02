package com.steevsapps.idledaddy.listeners

import android.util.Log
import `in`.dragonbra.javasteam.util.log.LogListener

/**
 * LogListener that prints to Android Logcat
 */
class AndroidLogListener : LogListener {
    override fun onLog(clazz: Class<*>, message: String?, throwable: Throwable?) =
        log(clazz, message, throwable, Log::i)

    override fun onError(clazz: Class<*>, message: String?, throwable: Throwable?) =
        log(clazz, message, throwable, Log::e)

    private fun log(
        clazz: Class<*>,
        message: String?,
        throwable: Throwable?,
        logType: (String, String, Throwable?) -> Int
    ) {
        val threadName = Thread.currentThread().name.take(10)
        val className = clazz.name
        val logMessage = if (message == null) {
            "[%10s] %s".format(threadName, className)
        } else {
            "[%10s] %s - %s".format(threadName, className, message)
        }
        logType(TAG, logMessage, throwable)
    }

    companion object {
        private const val TAG = "JavaSteam"
    }
}