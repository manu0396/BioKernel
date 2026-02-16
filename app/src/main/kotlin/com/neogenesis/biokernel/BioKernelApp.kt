package com.neogenesis.biokernel

import android.app.Application
import com.neogenesis.data.di.dataModule
import com.neogenesis.data_core.di.networkModule
import com.neogenesis.domain.di.domainModule
import com.neogenesis.feature_login.di.loginModule
import feature_dashboard.di.dashboardModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class BioKernelApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@BioKernelApp)
            modules(
                dataModule,
                domainModule,
                dashboardModule,
                loginModule,
                networkModule
            )
        }
    }
}



