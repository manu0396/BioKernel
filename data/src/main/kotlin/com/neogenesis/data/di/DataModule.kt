package com.neogenesis.data.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.neogenesis.data.db.BioKernelDatabase
import com.neogenesis.data.repository.LoginRepositoryImpl
import com.neogenesis.data.repository.MockRetinaRepository
import com.neogenesis.data.repository.RetinaRepositoryImpl
import com.neogenesis.domain.repository.LoginRepository
import com.neogenesis.domain.repository.RetinaRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    single<BioKernelDatabase> {
        val driver = AndroidSqliteDriver(
            schema = BioKernelDatabase.Schema,
            context = androidContext(),
            name = "biokernel.db"
        )
        BioKernelDatabase(driver)
    }
    singleOf(::MockRetinaRepository) { bind<RetinaRepository>() }
    singleOf(::LoginRepositoryImpl) { bind<LoginRepository>() }
    singleOf(::RetinaRepositoryImpl) { bind<RetinaRepository>() }
}



