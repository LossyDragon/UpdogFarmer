package com.steevsapps.idledaddy.utils

object WebHelpers {

    private fun isUrlSafeChar(ch: Char): Boolean {
        if (!(ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9')) {
            when (ch) {
                '-', '.', '_' -> return true
            }

            return false
        }

        return true
    }

    fun urlEncode(input: String): String = urlEncode(input.toByteArray())

    @JvmStatic
    fun urlEncode(input: ByteArray): String {
        val encoded = StringBuilder(input.size * 2)

        for (element in input) {
            val inch = Char(element.toUShort())

            if (isUrlSafeChar(inch)) {
                encoded.append(inch)
            } else if (inch == ' ') {
                encoded.append('+')
            } else {
                encoded.append(String.format("%%%02X", element))
            }
        }

        return encoded.toString()
    }
}
