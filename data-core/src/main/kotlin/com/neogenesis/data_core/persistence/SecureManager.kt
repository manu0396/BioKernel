package com.neogenesis.data_core.persistence

interface SecureKeyManager {
    fun getOrCreateDatabaseKey(): String
}


