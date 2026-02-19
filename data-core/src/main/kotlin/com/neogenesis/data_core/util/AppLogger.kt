package com.neogenesis.data_core.util

import android.util.Log
import com.neogenesis.datacore.BuildConfig

object AppLogger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d("BIO-DEBUG-$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e("BIO-ERROR-$tag", message, throwable)
        }
    }

    fun info(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i("BIO-INFO-$tag", message)
    }
}