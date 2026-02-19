package com.neogenesis.biokernel

import android.app.Application
import android.util.Log
import com.neogenesis.biokernel.di.appModule
import com.neogenesis.data.di.dataModule
import com.neogenesis.data.di.persistenceModule
import com.neogenesis.data_core.di.dataCoreModule
import com.neogenesis.domain.di.domainModule
import com.neogenesis.feature_login.di.loginModule
import com.neogenesis.session.di.sessionModule
import feature_dashboard.di.dashboardModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class BioKernelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@BioKernelApp)
            workManagerFactory()
            modules(
                dataCoreModule,
                persistenceModule,
                dataModule,
                domainModule,
                sessionModule,
                loginModule,
                dashboardModule,
                appModule
            )
        }
    }
}


