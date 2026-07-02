package com.steevsapps.idledaddy.steam.converter

import `in`.dragonbra.javasteam.types.KeyValue
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.reflect.Type

class VdfConverterFactory private constructor() : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation?>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type !is Class<*> || !KeyValue::class.java.isAssignableFrom(type)) {
            return null
        }
        return VdfConverter()
    }

    private class VdfConverter : Converter<ResponseBody, KeyValue> {
        @Throws(IOException::class)
        override fun convert(value: ResponseBody): KeyValue {
            val kv = KeyValue()
            kv.readAsText(ByteArrayInputStream(value.bytes()))
            return kv
        }
    }

    companion object {
        @JvmStatic
        fun create(): VdfConverterFactory = VdfConverterFactory()
    }
}
