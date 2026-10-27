package com.joakim.rfidmanager.nfc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FasadNfcBridgeTest {

    @Test
    fun `buildTargetUri bygger kontraktsenlig URI med mellanslag som procent20`() {
        val uri = FasadNfcBridge.buildTargetUri(
            lgh = "101",
            rum = "badrum 1",
            del = "kran"
        )
        assertEquals("fasad://besiktning?lgh=101&rum=badrum%201&del=kran", uri)
    }

    @Test
    fun `buildTargetUri hanterar svenska tecken och utrustning samt trimmar`() {
        val uri = FasadNfcBridge.buildTargetUri(
            lgh = " 1201 ",
            rum = " kök ",
            del = " ",
            utrustning = " kylskap "
        )
        assertEquals("fasad://besiktning?lgh=1201&rum=k%C3%B6k&utrustning=kylskap", uri)
        val parsed = FasadNfcBridge.parseFasadUri(uri)
        assertNotNull(parsed)
        assertEquals("1201", parsed!!.lgh)
        assertEquals("kök", parsed.rum)
        assertNull(parsed.del)
        assertEquals("kylskap", parsed.utrustning)
        assertEquals("lgh 1201 · kök · kylskap", parsed.summaryLabel())
    }

    @Test
    fun `buildTargetUri avvisar tom eller for lang lgh`() {
        assertNull(FasadNfcBridge.buildTargetUri(lgh = null, rum = "hall"))
        assertNull(FasadNfcBridge.buildTargetUri(lgh = "   ", rum = "hall"))
        assertNull(FasadNfcBridge.buildTargetUri(lgh = "12345678901234567", rum = "hall"))
        assertNotNull(FasadNfcBridge.buildTargetUri(lgh = "1234567890123456"))
    }

    @Test
    fun `appendUidToFasadUri lagger till eller ersatter uid i versalt hexformat`() {
        val base = "fasad://besiktning?lgh=101&rum=badrum%201&del=kran"
        val withUid = FasadNfcBridge.appendUidToFasadUri(base, "04a1b2c3d4e580")
        assertEquals(
            "fasad://besiktning?lgh=101&rum=badrum%201&del=kran&uid=04A1B2C3D4E580",
            withUid
        )

        val replaced = FasadNfcBridge.appendUidToFasadUri(withUid, "04998877665544")
        assertEquals(
            "fasad://besiktning?lgh=101&rum=badrum%201&del=kran&uid=04998877665544",
            replaced
        )
    }

    @Test
    fun `appendUidToFasadUri lamnar icke-Fasad-URI oforandrad`() {
        val other = "https://example.com/test?a=1"
        assertEquals(other, FasadNfcBridge.appendUidToFasadUri(other, "04A1B2C3D4E580"))
    }

    @Test
    fun `buildUidOnlyUri skapar Lage 2 URI for skrivskyddad tagg`() {
        val uri = FasadNfcBridge.buildUidOnlyUri("04a1b2c3d4e580")
        assertEquals("fasad://besiktning?uid=04A1B2C3D4E580", uri)
        assertTrue(FasadNfcBridge.isFasadUri(uri))
        val parsed = FasadNfcBridge.parseFasadUri(uri)
        assertNotNull(parsed)
        assertEquals("", parsed!!.lgh)
        assertEquals("04A1B2C3D4E580", parsed.uid)
        assertEquals("UID 04A1B2C3D4E580", parsed.summaryLabel())
    }

    @Test
    fun `NDEF TLV kodning till 4-bytes Ultralight-sidor och avkodning tillbaka`() {
        val uri = "fasad://besiktning?lgh=101&rum=badrum%201&del=kran"
        val pages = FasadNfcBridge.encodeUriToUltralightPages(uri)
        assertNotNull(pages)
        assertTrue(pages!!.keys.first() == 4)
        assertTrue(pages.values.all { it.size == 4 })

        // Kontrollera NDEF Type 2 TLV-header på sida 4: 0x03, len, 0xD1, 0x01
        val page4 = pages[4]!!
        assertEquals(0x03, page4[0].toInt() and 0xFF)
        assertEquals(0xD1, page4[2].toInt() and 0xFF)
        assertEquals(0x01, page4[3].toInt() and 0xFF)

        // Kontrollera att avkodning från 4-bytes sidor ger exakt samma URI
        val decoded = FasadNfcBridge.decodeFasadUriFromPages(pages)
        assertEquals(uri, decoded)
    }

    @Test
    fun `decodeFasadUriFromPages klarar 16-bytes readPages-block fran AndroidNfcManager`() {
        val uri = "fasad://besiktning?lgh=1201&rum=k%C3%B6k&utrustning=kylskap"
        val fourBytePages = FasadNfcBridge.encodeUriToUltralightPages(uri)!!

        // Simulera mifareU.readPages(startPage) som returnerar 16 byte (4 sidor) för 2, 4, 8, 12, 16, 20...
        val simulatedReadBlocks = LinkedHashMap<Int, ByteArray>()
        simulatedReadBlocks[2] = ByteArray(16) // sida 2..5 (men sida 4 skrivs över av block 4 nedan)
        for (startPage in listOf(4, 8, 12, 16, 20, 24)) {
            val block16 = ByteArray(16)
            for (k in 0..3) {
                val p = fourBytePages[startPage + k] ?: ByteArray(4)
                System.arraycopy(p, 0, block16, k * 4, 4)
            }
            simulatedReadBlocks[startPage] = block16
        }

        val hexSectors = simulatedReadBlocks.mapValues { (_, bytes) ->
            bytes.joinToString(" ") { "%02X".format(it) }
        }
        assertEquals(uri, FasadNfcBridge.decodeFasadUriFromPages(simulatedReadBlocks))
        assertEquals(uri, FasadNfcBridge.decodeFasadUriFromHexSectors(hexSectors))
    }

    @Test
    fun `decodeFasadUriFromPages klarar ren ASCII utan NDEF-header fran sida 4`() {
        val rawUri = "fasad://besiktning?lgh=101&rum=hall&del=dorr"
        val ascii = rawUri.toByteArray(Charsets.UTF_8)
        val pages = LinkedHashMap<Int, ByteArray>()
        val pageCount = (ascii.size + 3) / 4
        for (i in 0 until pageCount) {
            val buf = ByteArray(4)
            val len = minOf(4, ascii.size - i * 4)
            System.arraycopy(ascii, i * 4, buf, 0, len)
            pages[4 + i] = buf
        }
        assertEquals(rawUri, FasadNfcBridge.decodeFasadUriFromPages(pages))
    }

    @Test
    fun `encodeUriToUltralightPages avvisar ogiltig URI eller for litet sidutrymme`() {
        assertNull(FasadNfcBridge.encodeUriToUltralightPages("https://example.com"))
        assertNull(FasadNfcBridge.encodeUriToUltralightPages("fasad://besiktning?lgh=101", startPage = 3))
        assertNull(
            FasadNfcBridge.encodeUriToUltralightPages(
                "fasad://besiktning?lgh=101&rum=badrum%201&del=kran",
                startPage = 4,
                maxPage = 5
            )
        )
    }

    @Test
    fun `canWriteUriToTag kontrollerar TagLocks pa alla berorda sidor`() {
        val uri = "fasad://besiktning?lgh=101&rum=badrum%201&del=kran"
        val unlockedSectors = mapOf(2 to "00 00 00 00")
        assertTrue(FasadNfcBridge.canWriteUriToTag(uri, unlockedSectors))

        // LB0 = 0x01 låser sidorna 4..7 -> ska neka skrivning
        val lockedFirstBlock = mapOf(2 to "01 00 00 00")
        assertFalse(FasadNfcBridge.canWriteUriToTag(uri, lockedFirstBlock))

        // LB0 = 0x04 låser sidorna 12..15 -> ska neka skrivning
        val lockedMiddleBlock = mapOf(2 to "04 00 00 00")
        assertFalse(FasadNfcBridge.canWriteUriToTag(uri, lockedMiddleBlock))
    }

    @Test
    fun `WriteMode armFasad armerar 30s skrivfonster och knyter forsta lasta tagg`() {
        val uri = "fasad://besiktning?lgh=101&rum=hall&del=dorr"
        val t0 = 5_000_000L
        val state = WriteMode.armFasad(uri, t0)
        assertTrue(state is WriteModeState.Armed)
        val armed = state as WriteModeState.Armed
        assertEquals(uri, armed.request.fasadUri)
        assertEquals(30, WriteMode.remainingSeconds(armed, t0))

        val scannedUid = "04A1B2C3D4E580"
        val outcome = WriteMode.onTagRead(armed, scannedUid, t0 + 3_000)
        assertNotNull(outcome.execute)
        assertEquals(scannedUid, outcome.execute!!.uidHex)
        assertEquals(uri, outcome.execute!!.fasadUri)

        val finished = WriteMode.finish(outcome.state, ok = true)
        assertEquals(
            WriteModeState.Finished(scannedUid, FasadNfcBridge.USER_START_PAGE, WriteResult.WRITTEN),
            finished
        )
    }
}
