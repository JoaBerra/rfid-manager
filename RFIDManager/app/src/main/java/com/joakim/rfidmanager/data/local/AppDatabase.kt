package com.joakim.rfidmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.joakim.rfidmanager.data.local.dao.PersistedReadingDao
import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity

@Database(
    entities = [PersistedReadingEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun persistedReadingDao(): PersistedReadingDao
}