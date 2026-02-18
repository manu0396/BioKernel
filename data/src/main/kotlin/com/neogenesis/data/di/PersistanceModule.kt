package com.neogenesis.data.di

import com.neogenesis.data.db.BioKernelDatabase
import com.neogenesis.data_core.persistence.EncryptedDriverFactory
import com.neogenesis.data_core.persistence.SecureKeyManager
import org.koin.dsl.module

val persistenceModule = module {
    single {
        val factory: EncryptedDriverFactory = get()
        val keyManager: SecureKeyManager = get()
        val passphrase = keyManager.getOrCreateDatabaseKey()

        val driver = factory.createSecureDriver(
            schema = BioKernelDatabase.Schema,
            name = "biokernel_secure.db",
            pass = passphrase
        )
        BioKernelDatabase(driver)
    }

    single { BioKernelDatabase(get()) }
    single { get<BioKernelDatabase>().bioKernelQueries }
}


