package com.joakim.rfidmanager.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Riktiga Room-migreringar (ingen destruktiv reserv, se [DatabaseProvider]).
 *
 * v1 -> v2 (utkorg): fem nya kolumner. `transmitted` och `status` BEHÅLLS (ALTER TABLE ADD COLUMN är
 * den enklaste och säkraste vägen – ingen tabellombyggnad, ingen dataflytt, fungerar på alla
 * SQLite-versioner från minSdk 24; DROP COLUMN kräver SQLite 3.35/Android 14). Befintliga poster:
 * transmitted=1 -> SENT, transmitted=0 -> PENDING.
 *
 * SQL-satserna ligger som rena strängar i [MIGRATION_1_2_SQL] så att de kan granskas, enhetstestas
 * mot schemafilerna (Migration1To2SqlTest) och köras mot riktig SQLite av
 * tools/verify_migration_1_2.py.
 */
val MIGRATION_1_2_SQL: List<String> = listOf(
    "ALTER TABLE `persisted_readings` ADD COLUMN `outboxStatus` TEXT NOT NULL DEFAULT 'PENDING'",
    "ALTER TABLE `persisted_readings` ADD COLUMN `attempts` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `persisted_readings` ADD COLUMN `lastError` TEXT",
    "ALTER TABLE `persisted_readings` ADD COLUMN `lastAttemptAt` INTEGER",
    "ALTER TABLE `persisted_readings` ADD COLUMN `sentAt` INTEGER",
    "UPDATE `persisted_readings` SET `outboxStatus` = 'SENT' WHERE `transmitted` = 1",
    "UPDATE `persisted_readings` SET `outboxStatus` = 'PENDING' WHERE `transmitted` = 0",
    "CREATE INDEX IF NOT EXISTS `index_persisted_readings_outboxStatus_timestamp` ON `persisted_readings` (`outboxStatus`, `timestamp`)"
)

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2_SQL.forEach { db.execSQL(it) }
    }
}

object Migrations {
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
