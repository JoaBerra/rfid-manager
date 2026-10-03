package com.joakim.rfidmanager.outbox.core

/**
 * Generisk utkorg (outbox) – kärnan. Ren Kotlin: inga Android-, Room-, MQTT- eller
 * domänberoenden i detta paket (kontrolleras av CoreHasNoPlatformImportsTest), så att
 * koden kan kopieras/flyttas till ett annat projekt (t.ex. FASAD) och enhetstestas på JVM.
 *
 * Se docs/OUTBOX.md.
 */

/** Livscykel för en post i utkorgen. */
enum class OutboxStatus {
    /** Sparad, väntar på att skickas (inkl. poster som misslyckats men ska försökas igen). */
    PENDING,

    /** Mottagaren har bekräftat leveransen (ack). Slutstatus. */
    SENT,

    /** Gav upp efter max antal försök. Skickas bara igen efter uttrycklig återköning (requeue / 'skicka nu'). */
    FAILED
}

/**
 * En post som väntar i utkorgen, sedd av kärnan.
 *
 * @param id unikt, stabilt id för posten (används som idempotensnyckel av transporten).
 * @param item själva nyttolasten (t.ex. en avläsning).
 * @param createdAt när posten skapades (ms sedan epoch) – avgör ordningen.
 * @param attempts antal misslyckade försök hittills.
 * @param lastAttemptAt tidpunkt för senaste försök, eller null om inget gjorts.
 * @param lastError felorsak från senaste misslyckade försök, eller null.
 */
data class OutboxEntry<out T>(
    val id: Long,
    val item: T,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null
)

/** Resultat av ett sändningsförsök. */
sealed interface SendResult {
    /** Mottagaren har bekräftat leveransen (t.ex. MQTT QoS 1 PUBACK). */
    data object Acked : SendResult

    /** Försöket misslyckades eller bekräftelse uteblev. [error] visas för användaren. */
    data class Failed(val error: String) : SendResult
}
