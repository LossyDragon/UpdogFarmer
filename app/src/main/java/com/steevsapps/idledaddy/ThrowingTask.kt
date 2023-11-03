package com.steevsapps.idledaddy

fun interface ThrowingTask {
    @Throws(Exception::class)
    fun run()
}
