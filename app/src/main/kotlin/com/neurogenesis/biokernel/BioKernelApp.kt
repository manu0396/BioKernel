package com.neurogenesis.biokernel

import android.app.Application
import com.neurogenesis.data.di.dataModule
import com.neurogenesis.data_core.di.networkModule
import com.neurogenesis.domain.di.domainModule
import com.neurogenesis.feature_dashboard.di.dashboardModule
import com.neurogenesis.feature_login.di.loginModule
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