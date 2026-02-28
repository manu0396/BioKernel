package com.neogenesis.data.di

import app.cash.sqldelight.db.SqlDriver
import com.neogenesis.data.db.BioKernelDatabase
import com.neogenesis.data.repository.LoginRepositoryImpl
import com.neogenesis.data.repository.MockRetinaRepository
import com.neogenesis.data.repository.RetinaRepositoryImpl
import com.neogenesis.data_core.persistence.EncryptedDriverFactory
import com.neogenesis.data_core.persistence.SecureKeyManager
import com.neogenesis.domain.repository.LoginRepository
import com.neogenesis.domain.repository.RetinaRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dataModule = module {
    single<SqlDriver> {
        val factory: EncryptedDriverFactory = get()
        val keyManager: SecureKeyManager = get()
        factory.createSecureDriver(
            schema = BioKernelDatabase.Schema,
            name = "biokernel_secure.db",
            pass = keyManager.getOrCreateDatabaseKey()
        )
    }

    single { BioKernelDatabase(get()) }
    single { get<BioKernelDatabase>().bioKernelQueries }

    single<LoginRepository> { LoginRepositoryImpl(get(), get()) }
    single<RetinaRepository>(named("prod")) { RetinaRepositoryImpl(get(), get()) }
    single<RetinaRepository>(named("mock")) { MockRetinaRepository() }
    single<RetinaRepository> { get(named("prod")) }
}


