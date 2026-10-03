package com.joakim.rfidmanager.migration

import com.joakim.rfidmanager.data.migration.MigrationPlanner
import org.junit.Assert.*
import org.junit.Test

class MigrationPlannerTest {

    @Test fun emptyInput_emptyPlan() {
        val p = MigrationPlanner.plan(emptyList(), emptyList())
        assertTrue(p.toInsert.isEmpty()); assertEquals(0, p.remapped)
    }

    @Test fun distinctIds_keptAsIs() {
        val p = MigrationPlanner.plan(listOf(entity(1), entity(2), entity(3)), emptyList())
        assertEquals(listOf(1L, 2L, 3L), p.toInsert.map { it.id })
    }

    @Test fun identicalDuplicateInFile_collapsed() {
        val p = MigrationPlanner.plan(listOf(entity(1), entity(1)), emptyList())
        assertEquals(1, p.toInsert.size); assertEquals(1, p.collapsedDuplicates); assertEquals(0, p.remapped)
    }

    @Test fun duplicateInFile_transmittedVariantWins() {
        val p = MigrationPlanner.plan(listOf(entity(1), entity(1, transmitted = true)), emptyList())
        assertTrue(p.toInsert.single().transmitted)
    }

    @Test fun duplicateInFile_transmittedNotDowngraded() {
        val p = MigrationPlanner.plan(listOf(entity(1, transmitted = true), entity(1)), emptyList())
        assertTrue(p.toInsert.single().transmitted)
    }

    @Test fun sameIdDifferentContentInFile_secondGetsNewId_nothingLost() {
        val p = MigrationPlanner.plan(listOf(entity(1, uid = "A"), entity(1, uid = "B"), entity(2)), emptyList())
        assertEquals(3, p.toInsert.size)
        assertEquals(1, p.remapped)
        assertEquals(setOf("A", "B", "UID2"), p.toInsert.map { it.uidOrCode }.toSet())
        assertEquals(3, p.toInsert.map { it.id }.toSet().size)
        assertEquals(3L, p.toInsert.first { it.uidOrCode == "B" }.id) // högsta id (2) + 1
    }

    @Test fun alreadyInRoom_sameContent_skippedAndRoomStatusKept() {
        val inRoom = listOf(entity(1, transmitted = true))
        val p = MigrationPlanner.plan(listOf(entity(1, transmitted = false)), inRoom)
        assertTrue(p.toInsert.isEmpty()); assertEquals(1, p.alreadyPresent)
    }

    @Test fun idCollisionWithNewerRoomReading_importedGetsNewId() {
        val inRoom = listOf(entity(1, uid = "NYTT", ts = 9_000_000_000_000L))
        val p = MigrationPlanner.plan(listOf(entity(1, uid = "GAMMAL"), entity(2, uid = "GAMMAL2")), inRoom)
        assertEquals(2, p.toInsert.size)
        assertEquals(1, p.remapped)
        val ids = p.toInsert.map { it.id }
        assertTrue(1L !in ids)
        assertEquals(setOf("GAMMAL", "GAMMAL2"), p.toInsert.map { it.uidOrCode }.toSet())
        assertEquals(2L, p.toInsert.first { it.uidOrCode == "GAMMAL2" }.id) // oförändrat id bevaras
    }

    @Test fun missingOrNonPositiveId_getsFreshIdAboveMax() {
        val p = MigrationPlanner.plan(listOf(entity(0, uid = "A"), entity(10), entity(-5, uid = "B")), emptyList())
        assertEquals(3, p.toInsert.size); assertEquals(2, p.remapped)
        assertEquals(setOf(10L, 11L, 12L), p.toInsert.map { it.id }.toSet())
    }

    @Test fun largeHugeMillisIds_preserved() {
        val ids = listOf(1_700_000_000_001L, 1_700_000_000_002L)
        val p = MigrationPlanner.plan(ids.map { entity(it) }, emptyList())
        assertEquals(ids, p.toInsert.map { it.id })
    }
}
