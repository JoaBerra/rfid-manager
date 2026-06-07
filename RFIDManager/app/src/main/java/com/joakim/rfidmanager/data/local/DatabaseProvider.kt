package com.joakim.rfidmanager.data.local

import android.content.Context
import androidx.room.Room

/**
 * Enkel provider för AppDatabase.
 * Används för att undvika Hilt i det befintliga projektet.
 * Initieras tidigt i MainActivity.
 */
object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "rfid_manager_database"
            )
                .fallbackToDestructiveMigration() // För utveckling – ta bort i produktion
                .build()
            INSTANCE = instance
            instance
        }
    }
}
