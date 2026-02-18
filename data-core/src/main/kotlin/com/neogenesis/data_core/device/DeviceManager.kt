package com.neogenesis.data_core.device

import android.provider.Settings

class DeviceManager(private val context: android.content.Context) {
    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}






