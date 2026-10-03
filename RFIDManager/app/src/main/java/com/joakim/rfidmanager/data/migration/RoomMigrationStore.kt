package com.joakim.rfidmanager.data.migration

import android.util.Log
import androidx.room.withTransaction
import com.joakim.rfidmanager.data.local.AppDatabase
import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity

class RoomMigrationStore(private val db: AppDatabase) : ReadingMigrationStore {
    private val dao = db.persistedReadingDao()

    override suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction { block() }
    override suspend fun existing(): List<PersistedReadingEntity> = dao.getAllOnce()
    override suspend fun insertAllStrict(readings: List<PersistedReadingEntity>) = dao.insertAllStrict(readings)
}

object AndroidMigrationLog : MigrationLog {
    private const val TAG = "JsonToRoomMigration"
    override fun info(msg: String) { Log.i(TAG, msg) }
    override fun warn(msg: String, t: Throwable?) { Log.w(TAG, msg, t) }
    override fun error(msg: String, t: Throwable?) { Log.e(TAG, msg, t) }
}
