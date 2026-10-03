package com.joakim.rfidmanager.data.migration

import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** Minsta möjliga yta mot databasen så att migreringen kan testas på JVM utan Room. */
interface ReadingMigrationStore {
    /** Kör [block] i EN transaktion; kastar blocket så rullas allt tillbaka. */
    suspend fun <T> inTransaction(block: suspend () -> T): T
    suspend fun existing(): List<PersistedReadingEntity>
    suspend fun insertAllStrict(readings: List<PersistedReadingEntity>)
}

/** Loggning injiceras (android.util.Log är inte tillgänglig i JVM-tester). */
interface MigrationLog {
    fun info(msg: String)
    fun warn(msg: String, t: Throwable? = null)
    fun error(msg: String, t: Throwable? = null)
}

sealed class MigrationResult {
    /** Ingen readings.json fanns – inget att göra. */
    data object NotNeeded : MigrationResult()

    data class Migrated(
        val imported: Int,
        val alreadyPresent: Int,
        val collapsedDuplicates: Int,
        val remapped: Int,
        val skipped: Int,
        /** Filen efter namnbytet, eller null om namnbytet misslyckades (säkert, nästa start är idempotent). */
        val renamedTo: File?
    ) : MigrationResult()

    /** JSON-filen är orörd och används igen vid nästa start. */
    data class Failed(val message: String, val cause: Throwable?) : MigrationResult()
}

/**
 * Engångsmigrering readings.json -> Room.
 *
 * - Allt skrivs i EN transaktion och verifieras (alla planerade id finns) innan commit.
 * - Först efter lyckad commit döps filen om till readings.json.migrated
 *   (finns den redan används readings.json.migrated.<n>). Filen raderas aldrig.
 * - Vid fel (trasig JSON, DB-fel, verifieringsfel): inget ändras, filen ligger kvar,
 *   felet loggas och migreringen provas igen vid nästa appstart.
 * - Idempotent: körs den igen (t.ex. efter krasch mellan commit och namnbyte) hoppas
 *   redan inlästa poster över, se [MigrationPlanner].
 */
class JsonToRoomMigrator(
    private val jsonFile: File,
    private val store: ReadingMigrationStore,
    private val log: MigrationLog
) {
    suspend fun migrate(): MigrationResult {
        if (!jsonFile.exists()) return MigrationResult.NotNeeded
        return try {
            val parsed = JsonReadingParser.parse(jsonFile.readText(Charsets.UTF_8))

            val plan = if (parsed.readings.isEmpty()) {
                MigrationPlanner.Plan(emptyList(), 0, 0, 0)
            } else {
                store.inTransaction {
                    val plan = MigrationPlanner.plan(parsed.readings, store.existing())
                    if (plan.toInsert.isNotEmpty()) {
                        store.insertAllStrict(plan.toInsert)
                        val idsNow = store.existing().mapTo(HashSet()) { it.id }
                        val missing = plan.toInsert.count { it.id !in idsNow }
                        check(missing == 0) { "Verifiering misslyckades: $missing av ${plan.toInsert.size} poster saknas efter insert" }
                    }
                    plan
                }
            }

            val renamed = renameMigrated()
            val result = MigrationResult.Migrated(
                imported = plan.toInsert.size,
                alreadyPresent = plan.alreadyPresent,
                collapsedDuplicates = plan.collapsedDuplicates,
                remapped = plan.remapped,
                skipped = parsed.skipped,
                renamedTo = renamed
            )
            log.info(
                "JSON->Room klar: $result" +
                    if (parsed.skipped > 0) " Överhoppade poster: ${parsed.skipReasons}" else ""
            )
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("JSON->Room-migreringen misslyckades; ${jsonFile.name} lämnas orörd och provas igen vid nästa start", e)
            MigrationResult.Failed(e.message ?: e.javaClass.simpleName, e)
        }
    }

    private fun renameMigrated(): File? {
        var target = File(jsonFile.parentFile, jsonFile.name + ".migrated")
        var n = 1
        while (target.exists()) {
            target = File(jsonFile.parentFile, jsonFile.name + ".migrated." + n++)
        }
        return if (jsonFile.renameTo(target)) {
            target
        } else {
            log.warn("Data finns i Room men ${jsonFile.name} kunde inte döpas om; nästa start läser om den (idempotent)")
            null
        }
    }
}
