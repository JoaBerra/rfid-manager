package com.joakim.rfidmanager.data.local

import android.content.Context
import androidx.room.Room

/**
 * Enkel provider för AppDatabase.
 * Används för att undvika Hilt i det befintliga projektet.
 *
 * Ingen fallbackToDestructiveMigration: vid schemaändring ska en riktig Migration
 * skrivas (se app/schemas/), annars ska appen hellre misslyckas högljutt än radera data.
 */
object DatabaseProvider {

    const val DATABASE_NAME = "rfid_manager_database"

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build().also { INSTANCE = it }
        }
    }
}
