package com.neogenesis.data_core.persistence

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File

interface BiometricAuthManager {
    suspend fun authenticateAndGetKey(): Result<String>
}

class EncryptedDriverFactory(private val context: Context) {

    fun createSecureDriver(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        name: String,
        pass: String
    ): SqlDriver {
        SQLiteDatabase.loadLibs(context)
        val oldDbFile = context.getDatabasePath("biokernel.db")
        val secureDbFile = context.getDatabasePath(name)
        val secureDriver = AndroidSqliteDriver(
            schema = schema,
            context = context,
            name = name,
            factory = SupportFactory(pass.toByteArray())
        )
        if (oldDbFile.exists() && !secureDbFile.exists()) {
            migratePlaintextToCipher(oldDbFile, name, pass)
            oldDbFile.delete()
        }
        return secureDriver
    }

    private fun migratePlaintextToCipher(oldFile: File, newName: String, pass: String) {
        val database = SQLiteDatabase.openDatabase(
            oldFile.absolutePath,
            "",
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        database.rawExecSQL("ATTACH DATABASE '${context.getDatabasePath(newName).absolutePath}' AS encrypted KEY '$pass';")
        database.rawExecSQL("SELECT sqlcipher_export('encrypted');")
        database.rawExecSQL("DETACH DATABASE encrypted;")
        database.close()
    }
}


