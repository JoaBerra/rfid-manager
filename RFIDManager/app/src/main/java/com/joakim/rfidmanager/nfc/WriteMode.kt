package com.joakim.rfidmanager.nfc

/**
 * # Skrivläge för NFC – ren Kotlin (inga Android-beroenden, enhetstestad)
 *
 * Tidigare öppnades skrivformuläret (adress + data) så fort man markerade/sparade en tagg och
 * gick inte att stänga. Nu gäller: **en läsning visar/sparar bara läsningen.** Skrivläget startas
 * först när användaren trycker på knappen "Skriv till tagg" för den senast lästa taggen, eller när
 * Fasad begär en skyddad taggbindning via `fasad.rfid.WRITE_TAG` ([WriteMode.armFasad]).
 *
 * Tillstånd: [WriteModeState.Idle] → (knapp) [WriteModeState.Editing] → (Skriv) [WriteModeState.Armed]
 * → (taggen hålls mot telefonen) [WriteModeState.Finished]. Avbryt/tillbaka/timeout/ny tagg → Idle eller
 * Finished(TIMEOUT). Allt tillståndsbeslut finns här; MainActivity och ScanScreen bara visar/anropar.
 */
class WriteRequest(
    val uidHex: String,
    val addr: Int,
    val data: ByteArray,
    val isUltra: Boolean,
    val fasadUri: String? = null
) {
    fun withUid(newUidHex: String): WriteRequest =
        WriteRequest(uidHex = newUidHex, addr = addr, data = data, isUltra = isUltra, fasadUri = fasadUri)

    override fun equals(other: Any?): Boolean =
        other is WriteRequest && uidHex == other.uidHex && addr == other.addr &&
            isUltra == other.isUltra && fasadUri == other.fasadUri && data.contentEquals(other.data)

    override fun hashCode(): Int =
        31 * (31 * (31 * uidHex.hashCode() + addr) + data.contentHashCode()) + (fasadUri?.hashCode() ?: 0)
}

/** Den senast lästa taggen: [readAt] i ms, [writable] enligt [WriteMode.isWritable]. */
data class LastRead(val uidHex: String, val readAt: Long, val writable: Boolean)

enum class WriteResult { WRITTEN, FAILED, TIMEOUT }

sealed interface WriteModeState {
    /** Inget skrivläge. Läsning och sparande fungerar som vanligt. */
    object Idle : WriteModeState

    /** Användaren har tryckt "Skriv till tagg": formuläret (adress + data) visas för [uidHex]. */
    data class Editing(val uidHex: String) : WriteModeState

    /** Skrivning beställd; väntar på att taggen hålls mot telefonen tills [deadlineAt] (ms). */
    data class Armed(val request: WriteRequest, val deadlineAt: Long) : WriteModeState

    /** Skrivningen är klar (eller avbröts av tidsgräns). Visas som bekräftelse tills den stängs. */
    data class Finished(val uidHex: String, val addr: Int, val result: WriteResult) : WriteModeState
}

/** Resultat av att en tagg lästes medan skrivläget kanske var aktivt. */
data class TagReadOutcome(val state: WriteModeState, val execute: WriteRequest? = null)

object WriteMode {
    /** Så länge efter en läsning räknas taggen som "nyss läst" och knappen är aktiv. */
    const val RECENT_READ_MS = 120_000L

    /** Hur länge skrivläget väntar på att taggen hålls mot telefonen. */
    const val ARM_TIMEOUT_MS = 30_000L

    /** Platshållare för `uidHex` när en Fasad-taggbindning armeras innan taggen hållits mot telefonen. */
    const val ANY_TAG_UID = "*"

    fun isRecent(lastRead: LastRead?, now: Long): Boolean =
        lastRead != null && now - lastRead.readAt in 0..RECENT_READ_MS

    /** Knappen "Skriv till tagg" är aktiv: vilande läge, taggen är den senast lästa, nyss läst och skrivbar. */
    fun canStartWrite(state: WriteModeState, lastRead: LastRead?, uidHex: String, now: Long): Boolean =
        (state is WriteModeState.Idle || state is WriteModeState.Finished) &&
            lastRead != null && lastRead.uidHex == uidHex && lastRead.writable && isRecent(lastRead, now)

    /** Tryck på knappen: gå in i redigering om villkoren håller, annars oförändrat. */
    fun enter(state: WriteModeState, lastRead: LastRead?, uidHex: String, now: Long): WriteModeState =
        if (canStartWrite(state, lastRead, uidHex, now)) WriteModeState.Editing(uidHex) else state

    /** "Skriv" i formuläret: beställ skrivningen. Bara från Editing för samma tagg. */
    fun arm(state: WriteModeState, request: WriteRequest, now: Long): WriteModeState =
        if (state is WriteModeState.Editing && state.uidHex == request.uidHex)
            WriteModeState.Armed(request, now + ARM_TIMEOUT_MS)
        else state

    /**
     * Armera skrivning av en Fasad-URI (`fasad://besiktning?...`) med 30 sekunders tidsfönster.
     * Om [uidHex] är [ANY_TAG_UID] knyts den första skrivbara Ultralight/NTAG-tagg som hålls mot telefonen.
     */
    fun armFasad(fasadUri: String, now: Long, uidHex: String = ANY_TAG_UID): WriteModeState {
        val pages = FasadNfcBridge.encodeUriToUltralightPages(fasadUri) ?: return WriteModeState.Idle
        val firstPage = pages[FasadNfcBridge.USER_START_PAGE] ?: ByteArray(4)
        val req = WriteRequest(
            uidHex = uidHex,
            addr = FasadNfcBridge.USER_START_PAGE,
            data = firstPage,
            isUltra = true,
            fasadUri = fasadUri
        )
        return WriteModeState.Armed(req, now + ARM_TIMEOUT_MS)
    }

    /**
     * En tagg lästes. Är skrivläget beställt, tiden inte gått ut och det är samma tagg (eller [ANY_TAG_UID]
     * vid Fasad-taggbindning) → [TagReadOutcome.execute] anger skrivningen som ska utföras nu.
     * Annars: en annan tagg under redigering stänger formuläret, en bekräftelse rensas av nästa läsning,
     * en beställd skrivning till annan tagg väntar vidare.
     */
    fun onTagRead(state: WriteModeState, readUidHex: String, now: Long): TagReadOutcome = when (state) {
        is WriteModeState.Armed -> when {
            now >= state.deadlineAt -> TagReadOutcome(timedOut(state))
            state.request.uidHex == ANY_TAG_UID || readUidHex == state.request.uidHex -> {
                val resolved = if (state.request.uidHex == ANY_TAG_UID) {
                    state.request.withUid(readUidHex)
                } else {
                    state.request
                }
                TagReadOutcome(WriteModeState.Armed(resolved, state.deadlineAt), resolved)
            }
            else -> TagReadOutcome(state)
        }
        is WriteModeState.Editing ->
            TagReadOutcome(if (readUidHex == state.uidHex) state else WriteModeState.Idle)
        is WriteModeState.Finished -> TagReadOutcome(WriteModeState.Idle)
        WriteModeState.Idle -> TagReadOutcome(state)
    }

    /** Skrivningen utfördes (ok eller inte). */
    fun finish(state: WriteModeState, ok: Boolean): WriteModeState =
        if (state is WriteModeState.Armed)
            WriteModeState.Finished(state.request.uidHex, state.request.addr, if (ok) WriteResult.WRITTEN else WriteResult.FAILED)
        else state

    /** Klocktick: beställd skrivning som gått över tiden blir TIMEOUT. */
    fun tick(state: WriteModeState, now: Long): WriteModeState =
        if (state is WriteModeState.Armed && now >= state.deadlineAt) timedOut(state) else state

    /** Avbryt, tillbaka, appen i bakgrunden, skanning stoppad eller bekräftelsen stängd. */
    @Suppress("UNUSED_PARAMETER")
    fun cancel(state: WriteModeState): WriteModeState = WriteModeState.Idle

    /** Sekunder kvar innan en beställd skrivning avbryts (aldrig negativt), annars 0. */
    fun remainingSeconds(state: WriteModeState, now: Long): Int =
        if (state is WriteModeState.Armed) ((state.deadlineAt - now + 999) / 1000).coerceAtLeast(0).toInt() else 0

    private fun timedOut(armed: WriteModeState.Armed) =
        WriteModeState.Finished(armed.request.uidHex, armed.request.addr, WriteResult.TIMEOUT)

    /**
     * Går det att avgöra att taggen är skrivbar? Okänd typ → nej. Ultralight/NTAG: skrivbar om någon användarsida
     * (4–19) inte är låst enligt lock-byten på sida 2; saknas sida 2 går det inte att avgöra och taggen räknas
     * som skrivbar (försök tillåts). Classic: kan inte avgöras utan nyckel → skrivbar.
     */
    fun isWritable(type: String, fullSectors: Map<Int, String>): Boolean {
        val t = type.uppercase()
        return when {
            t.contains("ULTRALIGHT") || t.contains("NTAG") -> {
                if (fullSectors[2] == null) true
                else {
                    val locked = TagLocks.parseLockedPages(fullSectors)
                    (4..19).any { it !in locked }
                }
            }
            t.contains("CLASSIC") -> true
            else -> false
        }
    }
}
