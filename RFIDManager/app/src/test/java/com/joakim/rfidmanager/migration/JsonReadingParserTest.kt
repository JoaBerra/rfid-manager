package com.joakim.rfidmanager.migration

import com.joakim.rfidmanager.data.migration.JsonReadingParser
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Test

class JsonReadingParserTest {

    @Test fun emptyText_givesEmptyList() {
        val r = JsonReadingParser.parse("")
        assertEquals(0, r.readings.size); assertEquals(0, r.skipped)
    }

    @Test fun blankText_givesEmptyList() {
        assertEquals(0, JsonReadingParser.parse("  \n\t ").readings.size)
    }

    @Test fun emptyArray_givesEmptyList() {
        assertEquals(0, JsonReadingParser.parse("[]").readings.size)
    }

    @Test fun brokenJson_throws() {
        assertThrows(JSONException::class.java) { JsonReadingParser.parse("[{\"id\":1,\"type\":\"RFID\"") }
    }

    @Test fun notAnArray_throws() {
        assertThrows(JSONException::class.java) { JsonReadingParser.parse("{\"id\":1}") }
    }

    @Test fun garbage_throws() {
        assertThrows(JSONException::class.java) { JsonReadingParser.parse("hej, det här är inte JSON") }
    }

    @Test fun fullRecord_allFieldsMapped() {
        val json = """[{"id":42,"type":"RFID","uidOrCode":"04A1B2","timestamp":1700000000123,
            "status":"persisted","transmitted":false,"source":"NFC","dataPreview":"P4: 01 02",
            "memoryBank":3,"address":4,"length":16,"payload":"0102","sparkplugJson":"{\"a\":1}","correlationId":"c1"}]"""
        val e = JsonReadingParser.parse(json).readings.single()
        assertEquals(42L, e.id); assertEquals("RFID", e.type); assertEquals("04A1B2", e.uidOrCode)
        assertEquals(1700000000123L, e.timestamp); assertEquals("NFC", e.source)
        assertEquals("P4: 01 02", e.dataPreview); assertEquals(3, e.memoryBank)
        assertEquals(4, e.address); assertEquals(16, e.length); assertEquals("0102", e.payload)
        assertEquals("{\"a\":1}", e.sparkplugJson); assertEquals("c1", e.correlationId)
        assertFalse(e.transmitted)
    }

    @Test fun optionalFieldsMissing_becomeNullOrDefault() {
        val e = JsonReadingParser.parse("""[{"id":7,"type":"EAN","uidOrCode":"5907789123456","timestamp":5}]""").readings.single()
        assertNull(e.source); assertNull(e.dataPreview); assertNull(e.memoryBank); assertNull(e.address)
        assertNull(e.length); assertNull(e.payload); assertNull(e.sparkplugJson); assertNull(e.correlationId)
        assertEquals("persisted", e.status); assertFalse(e.transmitted)
    }

    @Test fun explicitNulls_becomeNull() {
        val e = JsonReadingParser.parse("""[{"id":7,"type":"EAN","uidOrCode":"x","timestamp":5,"source":null,"memoryBank":null,"payload":null}]""").readings.single()
        assertNull(e.source); assertNull(e.memoryBank); assertNull(e.payload)
    }

    @Test fun legacyStatus_isNormalized() {
        val e = JsonReadingParser.parse("""[{"id":1,"type":"RFID","uidOrCode":"a","timestamp":1,"transmitted":true,"status":"transmitted via Sparkplug"}]""").readings.single()
        assertEquals("transmitted", e.status); assertTrue(e.transmitted)
    }

    @Test fun missingId_becomesZero() {
        val e = JsonReadingParser.parse("""[{"type":"RFID","uidOrCode":"a","timestamp":1}]""").readings.single()
        assertEquals(0L, e.id)
    }

    @Test fun recordsMissingRequiredFields_areSkippedNotFatal() {
        val json = """[{"id":1,"type":"RFID","uidOrCode":"a","timestamp":1},
            {"id":2,"type":"RFID","timestamp":2},
            {"id":3,"uidOrCode":"c","timestamp":3},
            {"id":4,"type":"RFID","uidOrCode":"d"},
            "inte ett objekt",
            {"id":5,"type":"RFID","uidOrCode":"e","timestamp":5}]"""
        val r = JsonReadingParser.parse(json)
        assertEquals(listOf(1L, 5L), r.readings.map { it.id })
        assertEquals(4, r.skipped); assertEquals(4, r.skipReasons.size)
    }

    @Test fun bom_isTolerated() {
        assertEquals(1, JsonReadingParser.parse("\uFEFF[{\"id\":1,\"type\":\"RFID\",\"uidOrCode\":\"a\",\"timestamp\":1}]").readings.size)
    }

    @Test fun largeFile_5000records() {
        val sb = StringBuilder("[")
        for (i in 1..5000) {
            if (i > 1) sb.append(",")
            sb.append("""{"id":$i,"type":"RFID","uidOrCode":"U$i","timestamp":${1_700_000_000_000L + i},"payload":"AABBCC$i"}""")
        }
        sb.append("]")
        val r = JsonReadingParser.parse(sb.toString())
        assertEquals(5000, r.readings.size); assertEquals(0, r.skipped)
        assertEquals(5000L, r.readings.last().id)
    }

    @Test fun specialCharactersAndUnicode_preserved() {
        val e = JsonReadingParser.parse("""[{"id":1,"type":"RFID","uidOrCode":"å\"ä\\ö","timestamp":1,"source":"Plats: Lager Ö"}]""").readings.single()
        assertEquals("å\"ä\\ö", e.uidOrCode); assertEquals("Plats: Lager Ö", e.source)
    }
}
