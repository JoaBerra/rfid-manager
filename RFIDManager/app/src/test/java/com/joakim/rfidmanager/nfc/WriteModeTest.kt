package com.joakim.rfidmanager.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WriteModeTest {

    private val uid = "047B05CA885884"
    private val other = "04555C42376081"
    private val t0 = 1_000_000L
    private val read = LastRead(uid, t0, writable = true)
    private val request = WriteRequest(uid, 12, byteArrayOf(1, 2, 3, 4), isUltra = true)

    private fun editing() = WriteMode.enter(WriteModeState.Idle, read, uid, t0 + 1_000)
    private fun armed() = WriteMode.arm(editing(), request, t0 + 2_000)

    // --- en läsning startar aldrig skrivläge ---

    @Test fun `lasning ensam ger aldrig skrivlage`() {
        val out = WriteMode.onTagRead(WriteModeState.Idle, uid, t0)
        assertEquals(WriteModeState.Idle, out.state)
        assertNull(out.execute)
    }

    @Test fun `knappen ar inaktiv utan nyss last tagg`() {
        assertFalse(WriteMode.canStartWrite(WriteModeState.Idle, null, uid, t0))
    }

    @Test fun `knappen ar aktiv for nyss last skrivbar tagg`() {
        assertTrue(WriteMode.canStartWrite(WriteModeState.Idle, read, uid, t0 + 1_000))
    }

    @Test fun `knappen ar inaktiv for annan tagg an den senast lasta`() {
        assertFalse(WriteMode.canStartWrite(WriteModeState.Idle, read, other, t0))
    }

    @Test fun `knappen ar inaktiv for icke skrivbar tagg`() {
        assertFalse(WriteMode.canStartWrite(WriteModeState.Idle, read.copy(writable = false), uid, t0))
    }

    @Test fun `knappen blir inaktiv nar lasningen inte langre ar nyss`() {
        assertTrue(WriteMode.canStartWrite(WriteModeState.Idle, read, uid, t0 + WriteMode.RECENT_READ_MS))
        assertFalse(WriteMode.canStartWrite(WriteModeState.Idle, read, uid, t0 + WriteMode.RECENT_READ_MS + 1))
    }

    @Test fun `knappen ar inaktiv medan skrivlage redan ar aktivt`() {
        assertFalse(WriteMode.canStartWrite(WriteModeState.Editing(uid), read, uid, t0))
        assertFalse(WriteMode.canStartWrite(armed(), read, uid, t0))
    }

    // --- in i skrivläge ---

    @Test fun `tryck pa knappen ger Editing`() {
        assertEquals(WriteModeState.Editing(uid), editing())
    }

    @Test fun `tryck pa inaktiv knapp andrar ingenting`() {
        assertEquals(WriteModeState.Idle, WriteMode.enter(WriteModeState.Idle, read, other, t0))
        assertEquals(WriteModeState.Idle, WriteMode.enter(WriteModeState.Idle, null, uid, t0))
    }

    @Test fun `arm kraver Editing for samma tagg`() {
        assertEquals(WriteModeState.Idle, WriteMode.arm(WriteModeState.Idle, request, t0))
        assertEquals(WriteModeState.Editing(other), WriteMode.arm(WriteModeState.Editing(other), request, t0))
        val a = armed() as WriteModeState.Armed
        assertEquals(request, a.request)
        assertEquals(t0 + 2_000 + WriteMode.ARM_TIMEOUT_MS, a.deadlineAt)
    }

    // --- skrivning ---

    @Test fun `samma tagg som beställt utfor skrivningen`() {
        val out = WriteMode.onTagRead(armed(), uid, t0 + 5_000)
        assertEquals(request, out.execute)
    }

    @Test fun `annan tagg utfor inte skrivningen och skrivlaget star kvar`() {
        val a = armed()
        val out = WriteMode.onTagRead(a, other, t0 + 5_000)
        assertNull(out.execute)
        assertEquals(a, out.state)
    }

    @Test fun `efter skrivning visas bekraftelse`() {
        val ok = WriteMode.finish(armed(), true)
        assertEquals(WriteModeState.Finished(uid, 12, WriteResult.WRITTEN), ok)
        val bad = WriteMode.finish(armed(), false)
        assertEquals(WriteModeState.Finished(uid, 12, WriteResult.FAILED), bad)
    }

    @Test fun `finish utanfor Armed andrar inget`() {
        assertEquals(WriteModeState.Idle, WriteMode.finish(WriteModeState.Idle, true))
        assertEquals(WriteModeState.Editing(uid), WriteMode.finish(WriteModeState.Editing(uid), true))
    }

    @Test fun `ny lasning rensar bekraftelsen och startar inte skrivlage`() {
        val fin = WriteMode.finish(armed(), true)
        val out = WriteMode.onTagRead(fin, uid, t0 + 60_000)
        assertEquals(WriteModeState.Idle, out.state)
        assertNull(out.execute)
    }

    @Test fun `knappen kan anvandas igen efter bekraftelsen`() {
        val fin = WriteMode.finish(armed(), true)
        assertTrue(WriteMode.canStartWrite(fin, read, uid, t0 + 10_000))
    }

    // --- avbryt / timeout ---

    @Test fun `avbryt gar tillbaka till Idle fran alla lagen`() {
        assertEquals(WriteModeState.Idle, WriteMode.cancel(editing()))
        assertEquals(WriteModeState.Idle, WriteMode.cancel(armed()))
        assertEquals(WriteModeState.Idle, WriteMode.cancel(WriteMode.finish(armed(), true)))
    }

    @Test fun `avbrutet skrivlage utfor ingen skrivning vid nasta lasning`() {
        val out = WriteMode.onTagRead(WriteMode.cancel(armed()), uid, t0 + 5_000)
        assertNull(out.execute)
        assertEquals(WriteModeState.Idle, out.state)
    }

    @Test fun `timeout avbryter beställd skrivning`() {
        val a = armed() as WriteModeState.Armed
        assertEquals(a, WriteMode.tick(a, a.deadlineAt - 1))
        assertEquals(WriteModeState.Finished(uid, 12, WriteResult.TIMEOUT), WriteMode.tick(a, a.deadlineAt))
    }

    @Test fun `lasning efter timeout utfor inte skrivningen`() {
        val a = armed() as WriteModeState.Armed
        val out = WriteMode.onTagRead(a, uid, a.deadlineAt + 1)
        assertNull(out.execute)
        assertEquals(WriteResult.TIMEOUT, (out.state as WriteModeState.Finished).result)
    }

    @Test fun `tick andrar inte Idle eller Editing`() {
        assertEquals(WriteModeState.Idle, WriteMode.tick(WriteModeState.Idle, t0 + 10_000_000))
        assertEquals(WriteModeState.Editing(uid), WriteMode.tick(WriteModeState.Editing(uid), t0 + 10_000_000))
    }

    @Test fun `annan tagg under redigering stanger formularet`() {
        val out = WriteMode.onTagRead(editing(), other, t0 + 5_000)
        assertEquals(WriteModeState.Idle, out.state)
        assertEquals(WriteModeState.Editing(uid), WriteMode.onTagRead(editing(), uid, t0 + 5_000).state)
    }

    @Test fun `nedrakning i sekunder`() {
        val a = armed() as WriteModeState.Armed
        assertEquals(30, WriteMode.remainingSeconds(a, a.deadlineAt - 30_000))
        assertEquals(1, WriteMode.remainingSeconds(a, a.deadlineAt - 1))
        assertEquals(0, WriteMode.remainingSeconds(a, a.deadlineAt + 5))
        assertEquals(0, WriteMode.remainingSeconds(WriteModeState.Idle, t0))
    }

    // --- skrivbar? ---

    @Test fun `okand typ ar inte skrivbar`() {
        assertFalse(WriteMode.isWritable("UNKNOWN", emptyMap()))
    }

    @Test fun `Ultralight utan last-info raknas som skrivbar`() {
        assertTrue(WriteMode.isWritable("MIFARE Ultralight", mapOf(4 to "00 00 00 00")))
    }

    @Test fun `Ultralight med fria sidor ar skrivbar`() {
        assertTrue(WriteMode.isWritable("NTAG", mapOf(2 to "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")))
    }

    @Test fun `Ultralight med alla anvandarsidor last ar inte skrivbar`() {
        // LB0 = 0x0F låser sidorna 4–19
        assertFalse(WriteMode.isWritable("MIFARE Ultralight", mapOf(2 to "0F 00 00 00")))
    }

    @Test fun `Classic raknas som skrivbar`() {
        assertTrue(WriteMode.isWritable("MIFARE Classic 1K", emptyMap()))
    }

    @Test fun `lasta sidor tolkas som tidigare`() {
        val locked = TagLocks.parseLockedPages(mapOf(2 to "01 00 00 00"))
        assertEquals(setOf(4, 5, 6, 7), locked)
        assertTrue(TagLocks.isPageLocked(3, emptySet()))
        assertFalse(TagLocks.isPageLocked(12, locked))
    }
}
