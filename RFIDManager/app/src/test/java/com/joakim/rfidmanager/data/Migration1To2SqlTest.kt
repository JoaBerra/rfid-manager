package com.joakim.rfidmanager.data

import com.joakim.rfidmanager.data.local.MIGRATION_1_2
import com.joakim.rfidmanager.data.local.MIGRATION_1_2_SQL
import com.joakim.rfidmanager.data.local.Migrations
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * JVM-test av migreringen 1->2 utan SQLite-motor (MigrationTestHelper kräver instrumented test/enhet).
 * Testet kontrollerar att migreringens SQL är konsistent med de exporterade schemafilerna
 * 1.json och 2.json: exakt de kolumner/index som skiljer schemana läggs till, med exakt samma
 * definition som Room förväntar sig, och att transmitted->outboxStatus mappas som specat.
 *
 * Själva SQL-körningen mot riktig SQLite (befintliga rader SENT/PENDING) verifieras av
 * tools/verify_migration_1_2.py. Room validerar dessutom schemat (identityHash) vid första öppning.
 */
class Migration1To2SqlTest {

    private fun schema(version: Int): JSONObject {
        val rel = "schemas/com.joakim.rfidmanager.data.local.AppDatabase/$version.json"
        val f = listOf(File(rel), File("app/$rel")).first { it.isFile }
        return JSONObject(f.readText()).getJSONObject("database")
    }

    private fun entity(version: Int): JSONObject = schema(version).getJSONArray("entities").getJSONObject(0)

    /** Kolumndefinitioner i ordning, ur CREATE TABLE-satsen (utan PRIMARY KEY-delen). */
    private fun columns(createSql: String): List<String> {
        val inner = createSql.substringAfter("(").substringBeforeLast(")")
        return inner.split(", ").map { it.trim() }
    }

    @Test fun versionerna_och_migreringsobjektet_stämmer() {
        assertEquals(1, schema(1).getInt("version"))
        assertEquals(2, schema(2).getInt("version"))
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
        assertEquals(listOf(MIGRATION_1_2), Migrations.ALL.toList())
    }

    @Test fun lagda_kolumner_är_exakt_skillnaden_mellan_schema_1_och_2() {
        val v1 = columns(entity(1).getString("createSql"))
        val v2 = columns(entity(2).getString("createSql"))
        assertEquals("v2 börjar med alla v1-kolumner oförändrade", v1, v2.take(v1.size))
        val added = v2.drop(v1.size)
        val alterDefs = MIGRATION_1_2_SQL
            .filter { it.startsWith("ALTER TABLE `persisted_readings` ADD COLUMN ") }
            .map { it.removePrefix("ALTER TABLE `persisted_readings` ADD COLUMN ") }
        assertEquals(added, alterDefs)
        assertEquals(5, alterDefs.size)
    }

    @Test fun nytt_index_matchar_schema_2_och_transmitted_index_finns_kvar() {
        val v1idx = entity(1).getJSONArray("indices")
        val v2idx = entity(2).getJSONArray("indices")
        val v1names = (0 until v1idx.length()).map { v1idx.getJSONObject(it).getString("name") }
        val v2 = (0 until v2idx.length()).associate { v2idx.getJSONObject(it).getString("name") to v2idx.getJSONObject(it).getString("createSql") }
        assertTrue(v1names.all { it in v2 })
        val created = v2.filterKeys { it !in v1names }
        assertEquals(1, created.size)
        val expected = created.values.single().replace("\${TABLE_NAME}", "persisted_readings")
        assertTrue("saknar $expected", MIGRATION_1_2_SQL.contains(expected))
    }

    @Test fun transmitted_mappas_till_SENT_och_PENDING_och_ingen_data_raderas() {
        assertTrue(MIGRATION_1_2_SQL.any { it.contains("SET `outboxStatus` = 'SENT'") && it.contains("`transmitted` = 1") })
        assertTrue(MIGRATION_1_2_SQL.any { it.contains("SET `outboxStatus` = 'PENDING'") && it.contains("`transmitted` = 0") })
        assertTrue("migreringen får inte radera/ändra tabellen destruktivt",
            MIGRATION_1_2_SQL.none { it.contains("DROP", ignoreCase = true) || it.contains("DELETE", ignoreCase = true) })
    }

    @Test fun entiteten_deklarerar_samma_standardvärden_som_migreringen() {
        val v2 = columns(entity(2).getString("createSql"))
        assertTrue(v2.contains("`outboxStatus` TEXT NOT NULL DEFAULT 'PENDING'"))
        assertTrue(v2.contains("`attempts` INTEGER NOT NULL DEFAULT 0"))
    }
}
