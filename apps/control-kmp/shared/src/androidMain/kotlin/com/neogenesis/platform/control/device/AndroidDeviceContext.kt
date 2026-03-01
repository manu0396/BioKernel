package com.neogenesis.platform.control.device

import android.content.Context

object AndroidDeviceContext {
    @Volatile
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun get(): Context = requireNotNull(context) { "AndroidDeviceContext not initialized" }
}
