package com.joakim.rfidmanager.data.migration

import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity

/**
 * Ren logik som avgör vilka JSON-poster som ska in i Room, utan att tappa data.
 *
 * Regler (id bevaras när det går):
 * 1. Samma id som redan finns (i Room eller tidigare i filen) och samma innehåll
 *    (alla fält utom id/status/transmitted) = samma avläsning => hoppas över
 *    (idempotent; Rooms ev. nyare status rörs inte). Dubblett i filen där den senare
 *    är markerad som skickad behåller den skickade varianten.
 * 2. Samma id men olika innehåll => verklig id-krock (t.ex. om nya avläsningar sparats i
 *    Room efter en misslyckad migrering). Den inlästa posten får ett nytt id
 *    (högsta kända + 1) så ingen av dem skrivs över.
 * 3. id <= 0 (saknas) => får nytt id.
 */
object MigrationPlanner {

    data class Plan(
        val toInsert: List<PersistedReadingEntity>,
        /** Poster som redan fanns i Room (samma id och innehåll). */
        val alreadyPresent: Int,
        /** Dubbletter inom filen som slogs ihop. */
        val collapsedDuplicates: Int,
        /** Poster som fick nytt id pga krock eller saknat id. */
        val remapped: Int
    )

    fun plan(parsed: List<PersistedReadingEntity>, existing: List<PersistedReadingEntity>): Plan {
        val existingById = existing.associateBy { it.id }
        val pendingById = LinkedHashMap<Long, PersistedReadingEntity>()
        var maxId = maxOf(
            existing.maxOfOrNull { it.id } ?: 0L,
            parsed.maxOfOrNull { it.id } ?: 0L,
            0L
        )
        var alreadyPresent = 0
        var collapsed = 0
        var remapped = 0

        for (r in parsed) {
            if (r.id <= 0L) {
                val newId = ++maxId
                pendingById[newId] = r.copy(id = newId)
                remapped++
                continue
            }
            val inRoom = existingById[r.id]
            val inFile = pendingById[r.id]
            when {
                inRoom != null && sameReading(inRoom, r) -> alreadyPresent++
                inFile != null && sameReading(inFile, r) -> {
                    collapsed++
                    if (r.transmitted && !inFile.transmitted) pendingById[r.id] = r
                }
                inRoom != null || inFile != null -> {
                    val newId = ++maxId
                    pendingById[newId] = r.copy(id = newId)
                    remapped++
                }
                else -> pendingById[r.id] = r
            }
        }
        return Plan(pendingById.values.toList(), alreadyPresent, collapsed, remapped)
    }

    /** Samma avläsning = alla fält utom id, status och transmitted är lika. */
    fun sameReading(a: PersistedReadingEntity, b: PersistedReadingEntity): Boolean =
        a.type == b.type &&
            a.uidOrCode == b.uidOrCode &&
            a.timestamp == b.timestamp &&
            a.source == b.source &&
            a.dataPreview == b.dataPreview &&
            a.memoryBank == b.memoryBank &&
            a.address == b.address &&
            a.length == b.length &&
            a.payload == b.payload &&
            a.sparkplugJson == b.sparkplugJson &&
            a.correlationId == b.correlationId
}
