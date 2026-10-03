package com.joakim.rfidmanager.migration

import com.joakim.rfidmanager.data.migration.JsonToRoomMigrator
import com.joakim.rfidmanager.data.migration.MigrationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JsonToRoomMigratorTest {

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var json: File
    private lateinit var log: RecordingLog

    @Before fun setUp() {
        json = File(tmp.root, "readings.json")
        log = RecordingLog()
    }

    private fun migrator(store: FakeStore) = JsonToRoomMigrator(json, store, log)
    private fun migrated() = File(tmp.root, "readings.json.migrated")

    private fun rec(id: Long, uid: String = "U$id", extra: String = "") =
        """{"id":$id,"type":"RFID","uidOrCode":"$uid","timestamp":${1_700_000_000_000L + id}$extra}"""

    @Test fun noFile_notNeeded() = runBlocking {
        val store = FakeStore()
        assertEquals(MigrationResult.NotNeeded, migrator(store).migrate())
        assertEquals(0, store.transactions)
    }

    @Test fun emptyFile_renamedNothingImported() = runBlocking {
        json.writeText("")
        val store = FakeStore()
        val r = migrator(store).migrate() as MigrationResult.Migrated
        assertEquals(0, r.imported); assertEquals(0, store.rows.size)
        assertFalse(json.exists()); assertTrue(migrated().exists())
    }

    @Test fun emptyArray_renamed() = runBlocking {
        json.writeText("[]")
        val r = migrator(FakeStore()).migrate()
        assertTrue(r is MigrationResult.Migrated)
        assertTrue(migrated().exists())
    }

    @Test fun brokenJson_failsKeepsFileAndDb_logs() = runBlocking {
        val broken = "[{\"id\":1,\"type\":\"RFID\",\"uidOrCode\":\"a\",\"timesta"
        json.writeText(broken)
        val store = FakeStore()
        val r = migrator(store).migrate()
        assertTrue(r is MigrationResult.Failed)
        assertTrue(json.exists()); assertEquals(broken, json.readText())
        assertFalse(migrated().exists()); assertEquals(0, store.rows.size)
        assertEquals(1, log.errors.size)
    }

    @Test fun brokenJson_thenFixed_retrySucceeds() = runBlocking {
        json.writeText("[{")
        val store = FakeStore()
        assertTrue(migrator(store).migrate() is MigrationResult.Failed)
        json.writeText("[${rec(1)}]")
        assertTrue(migrator(store).migrate() is MigrationResult.Migrated)
        assertEquals(1, store.rows.size)
    }

    @Test fun normalFile_importedPreservingIds_andRenamed() = runBlocking {
        json.writeText("[${rec(5)},${rec(9, extra = ",\"transmitted\":true,\"status\":\"transmitted via Sparkplug\"")}]")
        val store = FakeStore()
        val r = migrator(store).migrate() as MigrationResult.Migrated
        assertEquals(2, r.imported)
        assertEquals(setOf(5L, 9L), store.rows.keys)
        assertEquals("transmitted", store.rows[9]!!.status); assertTrue(store.rows[9]!!.transmitted)
        assertFalse(json.exists()); assertTrue(migrated().exists())
        assertEquals(migrated(), r.renamedTo)
        assertEquals(1, store.transactions); assertEquals(1, store.committed)
    }

    @Test fun optionalFieldsMissing_stillImported() = runBlocking {
        json.writeText("""[{"id":3,"type":"EAN","uidOrCode":"123","timestamp":9}]""")
        val store = FakeStore()
        migrator(store).migrate()
        val e = store.rows[3L]!!
        assertNull(e.source); assertNull(e.payload); assertEquals("persisted", e.status)
    }

    @Test fun largeFile_5000records_oneTransaction() = runBlocking {
        json.writeText((1..5000).joinToString(",", "[", "]") { rec(it.toLong(), extra = ",\"payload\":\"AA$it\"") })
        val store = FakeStore()
        val r = migrator(store).migrate() as MigrationResult.Migrated
        assertEquals(5000, r.imported); assertEquals(5000, store.rows.size)
        assertEquals(1, store.transactions); assertEquals(1, store.committed)
        assertEquals("AA4711", store.rows[4711L]!!.payload)
        assertTrue(migrated().exists())
    }

    @Test fun duplicatesInFile_handledNoLoss() = runBlocking {
        json.writeText("[${rec(1)},${rec(1)},${rec(1, uid = "ANNAN")}]")
        val store = FakeStore()
        val r = migrator(store).migrate() as MigrationResult.Migrated
        assertEquals(2, store.rows.size)
        assertEquals(1, r.collapsedDuplicates); assertEquals(1, r.remapped)
        assertEquals(setOf("U1", "ANNAN"), store.rows.values.map { it.uidOrCode }.toSet())
    }

    @Test fun partlyBadRecords_skippedRestImported_fileKept() = runBlocking {
        json.writeText("""[${rec(1)},{"id":2,"type":"RFID"},${rec(3)}]""")
        val store = FakeStore()
        val r = migrator(store).migrate() as MigrationResult.Migrated
        assertEquals(2, r.imported); assertEquals(1, r.skipped)
        assertTrue(migrated().exists()) // originalet bevaras (inkl. den trasiga posten)
        assertTrue(migrated().readText().contains("\"id\":2"))
    }

    @Test fun idempotent_secondRunDoesNothing() = runBlocking {
        json.writeText("[${rec(1)},${rec(2)}]")
        val store = FakeStore()
        migrator(store).migrate()
        val after = store.rows.toMap()
        assertEquals(MigrationResult.NotNeeded, migrator(store).migrate())
        assertEquals(after, store.rows)
    }

    @Test fun crashAfterCommitBeforeRename_rerunIsIdempotent_keepsRoomChanges() = runBlocking {
        json.writeText("[${rec(1)},${rec(2)}]")
        val store = FakeStore()
        migrator(store).migrate()
        // Återskapa läget "commit gjord men filen ej omdöpt", och markera rad 1 som skickad i Room
        migrated().renameTo(json)
        store.rows[1L] = store.rows[1L]!!.copy(transmitted = true, status = "transmitted")
        val r = migrator(store).migrate() as MigrationResult.Migrated
        assertEquals(0, r.imported); assertEquals(2, r.alreadyPresent)
        assertEquals(2, store.rows.size)
        assertTrue(store.rows[1L]!!.transmitted)
        assertFalse(json.exists())
        assertTrue(File(tmp.root, "readings.json.migrated").exists())
    }

    @Test fun renameTargetExists_usesNumberedName_neverOverwrites() = runBlocking {
        migrated().writeText("GAMMALT INNEHÅLL")
        json.writeText("[${rec(1)}]")
        val r = migrator(FakeStore()).migrate() as MigrationResult.Migrated
        assertEquals("GAMMALT INNEHÅLL", migrated().readText())
        assertEquals(File(tmp.root, "readings.json.migrated.1"), r.renamedTo)
        assertTrue(r.renamedTo!!.exists())
    }

    @Test fun dbInsertFails_rolledBack_jsonKept_logged() = runBlocking {
        json.writeText("[${rec(1)},${rec(2)}]")
        val store = FakeStore(listOf(entity(100)))
        store.failOnInsert = RuntimeException("disk full")
        val r = migrator(store).migrate()
        assertTrue(r is MigrationResult.Failed)
        assertEquals("disk full", (r as MigrationResult.Failed).message)
        assertEquals(setOf(100L), store.rows.keys)
        assertTrue(json.exists()); assertFalse(migrated().exists())
        assertEquals(1, log.errors.size)
    }

    @Test fun verificationDetectsMissingRows_rollsBack() = runBlocking {
        json.writeText("[${rec(1)},${rec(2)}]")
        val store = FakeStore()
        store.dropOnInsert = true
        assertTrue(migrator(store).migrate() is MigrationResult.Failed)
        assertEquals(0, store.rows.size); assertTrue(json.exists())
    }

    @Test fun failedThenRetry_withNewRoomReadingsInBetween_noLossNoOverwrite() = runBlocking {
        json.writeText("[${rec(1, "GAMMAL1")},${rec(2, "GAMMAL2")}]")
        val store = FakeStore()
        store.failOnInsert = RuntimeException("tillfälligt fel")
        assertTrue(migrator(store).migrate() is MigrationResult.Failed)
        // appen fortsätter: nya avläsningar hamnar i Room med autogenererade id 1,2
        store.failOnInsert = null
        store.rows[1L] = entity(1, uid = "NY1", ts = 9_000_000_000_001L)
        store.rows[2L] = entity(2, uid = "NY2", ts = 9_000_000_000_002L)
        val r = migrator(store).migrate() as MigrationResult.Migrated
        assertEquals(2, r.imported); assertEquals(2, r.remapped)
        assertEquals(setOf("NY1", "NY2", "GAMMAL1", "GAMMAL2"), store.rows.values.map { it.uidOrCode }.toSet())
        assertEquals("NY1", store.rows[1L]!!.uidOrCode)
        assertTrue(migrated().exists())
    }

    @Test fun jsonFileIsNeverDeleted_inAnyOutcome() = runBlocking {
        json.writeText("[${rec(1)}]")
        migrator(FakeStore()).migrate()
        val files = tmp.root.listFiles()!!.map { it.name }
        assertEquals(listOf("readings.json.migrated"), files)
    }
}
