package com.joakim.rfidmanager.nfc

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.TreeMap

/**
 * # Fasad NFC-brygga — ren Kotlin (inga Android-beroenden, 100 % JVM-testbar)
 *
 * Implementerar gränssnittskontraktet i `wiki/Fasad-NFC-Integration.md`:
 * - Bygga och tolka läs-URI `fasad://besiktning?lgh=...&rum=...&del=...&utrustning=...&uid=...`
 * - Hänga på `uid` som närvarokvittens vid avläsning (`appendUidToFasadUri`)
 * - Bygga UID-endast-URI för skrivskyddade taggar i Läge 2 (`buildUidOnlyUri`)
 * - Koda och avkoda NDEF Type 2 Tag TLV (URI-record `'U'`) till/från MIFARE Ultralight / NTAG-sidor (4 byte per sida från sida 4)
 * - Kontrollera med [TagLocks] att alla sidor som krävs för skrivning är olåsta
 *
 * Ingen kännedom om Fasads rumslistor, protokollschema eller SQLite-databas.
 */
data class FasadTagTarget(
    val lgh: String,
    val rum: String? = null,
    val del: String? = null,
    val utrustning: String? = null,
    val uid: String? = null
) {
    /** Kort sammanfattning för UI-banner och taggkort, t.ex. `lgh 101 · badrum 1 · kran`. */
    fun summaryLabel(): String = buildList {
        if (lgh.isNotBlank()) add("lgh $lgh")
        if (!rum.isNullOrBlank()) add(rum)
        if (!del.isNullOrBlank()) add(del)
        if (!utrustning.isNullOrBlank()) add(utrustning)
        if (lgh.isBlank() && !uid.isNullOrBlank()) add("UID $uid")
    }.joinToString(" · ")
}

object FasadNfcBridge {
    const val ACTION_WRITE_TAG = "fasad.rfid.WRITE_TAG"
    const val SCHEME = "fasad"
    const val HOST = "besiktning"
    const val BASE_URI = "$SCHEME://$HOST"

    const val EXTRA_LGH = "lgh"
    const val EXTRA_RUM = "rum"
    const val EXTRA_DEL = "del"
    const val EXTRA_UTRUSTNING = "utrustning"
    const val EXTRA_UID = "uid"
    const val EXTRA_URI = "uri"

    const val FASAD_PACKAGE = "fasad.besiktning"
    const val FASAD_LABB_PACKAGE = "fasad.besiktning.labb"

    /** Första användarsidan på MIFARE Ultralight / NTAG (Type 2 Tag). */
    const val USER_START_PAGE = 4

    /** Sista användarsidan på NTAG213 (sidorna 4..39 = 36 sidor = 144 byte). */
    const val MAX_USER_PAGE = 39

    const val MAX_LGH_LENGTH = 16

    /**
     * Bygger `fasad://besiktning?lgh=...&rum=...&del=...&utrustning=...`.
     * Kräver att [lgh] efter trimning är 1–16 tecken. Tomma valfria parametrar utelämnas.
     * Mellanslag URL-kodas som `%20`.
     */
    fun buildTargetUri(
        lgh: String?,
        rum: String? = null,
        del: String? = null,
        utrustning: String? = null
    ): String? {
        val cleanLgh = lgh?.trim().orEmpty()
        if (cleanLgh.isEmpty() || cleanLgh.length > MAX_LGH_LENGTH) return null

        val params = mutableListOf<Pair<String, String>>()
        params += EXTRA_LGH to cleanLgh

        val cleanRum = rum?.trim().orEmpty()
        if (cleanRum.isNotEmpty()) params += EXTRA_RUM to cleanRum

        val cleanDel = del?.trim().orEmpty()
        if (cleanDel.isNotEmpty()) params += EXTRA_DEL to cleanDel

        val cleanUtrustning = utrustning?.trim().orEmpty()
        if (cleanUtrustning.isNotEmpty()) params += EXTRA_UTRUSTNING to cleanUtrustning

        val query = params.joinToString("&") { (k, v) -> "$k=${urlEncode(v)}" }
        return "$BASE_URI?$query"
    }

    /**
     * Om [uri] är en giltig `fasad://besiktning...`-URI läggs query-parametern `uid=<UPPERCASE_HEX>`
     * till (eller ersätter befintlig `uid`) som närvarokvittens. Annars returneras [uri] oförändrad.
     */
    fun appendUidToFasadUri(uri: String, uidHex: String): String {
        val trimmedUri = uri.trim()
        if (!isFasadUri(trimmedUri)) return uri
        val cleanUid = normalizeUidHex(uidHex)
        if (cleanUid.isEmpty()) return trimmedUri

        val rawQuery = trimmedUri.substringAfter('?', "")
        val preservedPairs = if (rawQuery.isBlank()) {
            emptyList()
        } else {
            rawQuery.split('&').mapNotNull { part ->
                val trimmedPart = part.trim()
                if (trimmedPart.isEmpty()) return@mapNotNull null
                val key = trimmedPart.substringBefore('=').trim()
                if (key.isEmpty() || key.equals(EXTRA_UID, ignoreCase = true)) null else trimmedPart
            }
        }
        val allPairs = preservedPairs + "$EXTRA_UID=${urlEncode(cleanUid)}"
        return "$BASE_URI?${allPairs.joinToString("&")}"
    }

    /**
     * Bygger `fasad://besiktning?uid=<UPPERCASE_HEX>` för Läge 2 (skrivskyddade taggar med enbart `tagUid`).
     */
    fun buildUidOnlyUri(uidHex: String): String {
        val cleanUid = normalizeUidHex(uidHex)
        return "$BASE_URI?$EXTRA_UID=${urlEncode(cleanUid)}"
    }

    /** Kontrollerar om [uri] är en giltig `fasad://besiktning`-URI med minst `lgh` (1–16 tecken) eller `uid`. */
    fun isFasadUri(uri: String?): Boolean = parseFasadUri(uri) != null

    /** Tolkar en `fasad://besiktning?...`-URI till [FasadTagTarget], eller `null` om ogiltig. */
    fun parseFasadUri(uri: String?): FasadTagTarget? {
        if (uri.isNullOrBlank()) return null
        val trimmed = uri.trim()
        val prefix = BASE_URI
        if (!trimmed.regionMatches(0, prefix, 0, prefix.length, ignoreCase = true)) return null
        val rest = trimmed.substring(prefix.length)
        val query = when {
            rest.isEmpty() -> ""
            rest.startsWith("?") -> rest.substring(1)
            rest.startsWith("/?") -> rest.substring(2)
            else -> return null
        }
        if (query.isEmpty()) return null

        val params = LinkedHashMap<String, String>()
        for (part in query.split('&')) {
            if (part.isBlank()) continue
            val eqIdx = part.indexOf('=')
            if (eqIdx <= 0) continue
            val key = urlDecode(part.substring(0, eqIdx)).trim().lowercase()
            val value = urlDecode(part.substring(eqIdx + 1)).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                params[key] = value
            }
        }

        val lgh = params[EXTRA_LGH].orEmpty()
        val uid = params[EXTRA_UID]?.let { normalizeUidHex(it) }?.takeIf { it.isNotEmpty() }

        if (lgh.length > MAX_LGH_LENGTH) return null
        if (lgh.isEmpty() && uid == null) return null

        return FasadTagTarget(
            lgh = lgh,
            rum = params[EXTRA_RUM],
            del = params[EXTRA_DEL],
            utrustning = params[EXTRA_UTRUSTNING],
            uid = uid
        )
    }

    /**
     * Packar en `fasad://besiktning...`-URI som standard NDEF Type 2 Tag TLV med URI-record (`'U'`, prefix `0x00`)
     * i 4-bytes sidor från [startPage] (standard sida 4) upp till högst [maxPage].
     *
     * Layout från sida 4:
     * `[0x03, ndefLen, 0xD1, 0x01, payloadLen, 0x55 ('U'), 0x00, ...utf8Bytes..., 0xFE]`
     * utfyllt med `0x00` till jämna 4-bytes sidor.
     *
     * Returnerar `null` om [uri] inte är en giltig Fasad-URI, om [startPage] < 4 eller om URI:n inte får plats.
     */
    fun encodeUriToUltralightPages(
        uri: String,
        startPage: Int = USER_START_PAGE,
        maxPage: Int = MAX_USER_PAGE
    ): Map<Int, ByteArray>? {
        val trimmed = uri.trim()
        if (startPage < USER_START_PAGE || maxPage < startPage) return null
        if (!isFasadUri(trimmed)) return null

        val uriBytes = trimmed.toByteArray(Charsets.UTF_8)
        val payloadLen = uriBytes.size + 1 // 1 byte identifier code (0x00) + URI UTF-8
        val ndefLen = 5 + uriBytes.size    // D1, 01, payloadLen, 55, 00 + URI
        if (payloadLen > 254 || ndefLen > 254) return null

        val tlv = ByteArray(2 + ndefLen + 1)
        tlv[0] = 0x03.toByte()             // NDEF Message TLV
        tlv[1] = ndefLen.toByte()          // TLV length
        tlv[2] = 0xD1.toByte()             // MB=1, ME=1, CF=0, SR=1, IL=0, TNF=0x01 (Well-Known)
        tlv[3] = 0x01.toByte()             // Type Length = 1
        tlv[4] = payloadLen.toByte()       // Payload Length
        tlv[5] = 0x55.toByte()             // Type = 'U' (URI)
        tlv[6] = 0x00.toByte()             // URI Identifier Code = 0x00 (no prefix)
        System.arraycopy(uriBytes, 0, tlv, 7, uriBytes.size)
        tlv[7 + uriBytes.size] = 0xFE.toByte() // Terminator TLV

        val pageCount = (tlv.size + 3) / 4
        val lastPage = startPage + pageCount - 1
        if (lastPage > maxPage) return null

        val pages = LinkedHashMap<Int, ByteArray>(pageCount)
        for (i in 0 until pageCount) {
            val pageBytes = ByteArray(4)
            val offset = i * 4
            val copyLen = minOf(4, tlv.size - offset)
            System.arraycopy(tlv, offset, pageBytes, 0, copyLen)
            pages[startPage + i] = pageBytes
        }
        return pages
    }

    /**
     * Avkodar `fasad://besiktning...` ur lästa Ultralight/NTAG-sidor.
     *
     * Stödjer både:
     * - 4-bytes sidor (`4 -> 4B, 5 -> 4B, ...`) och 16-bytes `readPages`-block (`2 -> 16B, 4 -> 16B, 8 -> 16B, ...`)
     * - NDEF Type 2 Tag TLV med URI-record (`0x03 ... 'U'`)
     * - Ren ASCII/UTF-8 `fasad://besiktning...` från sida 4
     */
    fun decodeFasadUriFromPages(pages: Map<Int, ByteArray>): String? {
        if (pages.isEmpty()) return null
        val raw = flattenUserPages(pages)
        if (raw.isEmpty()) return null

        decodeNdefUriFromTlv(raw)?.let { candidate ->
            if (isFasadUri(candidate)) return candidate
        }
        decodeRawAsciiFasadUri(raw)?.let { candidate ->
            if (isFasadUri(candidate)) return candidate
        }
        return null
    }

    /**
     * Avkodar `fasad://besiktning...` från UI-modellens `fullSectors` (`Map<Int, String>` med hex-bytes).
     */
    fun decodeFasadUriFromHexSectors(fullSectors: Map<Int, String>): String? {
        if (fullSectors.isEmpty()) return null
        val byteMap = LinkedHashMap<Int, ByteArray>()
        for ((page, hex) in fullSectors) {
            val bytes = hex.trim().split(Regex("\\s+")).mapNotNull { token ->
                token.toIntOrNull(16)?.toByte()
            }.toByteArray()
            if (bytes.isNotEmpty()) {
                byteMap[page] = bytes
            }
        }
        return decodeFasadUriFromPages(byteMap)
    }

    /**
     * Kontrollerar att samtliga [requiredPages] är skrivbara enligt [lockedPages] (från [TagLocks.parseLockedPages]).
     */
    fun canWritePages(requiredPages: Set<Int>, lockedPages: Set<Int>): Boolean =
        requiredPages.isNotEmpty() && requiredPages.none { TagLocks.isPageLocked(it, lockedPages) }

    /**
     * Kontrollerar att [uri] kan kodas till sidor från [startPage] och att ingen av dessa sidor är låst i [fullSectors].
     */
    fun canWriteUriToTag(
        uri: String,
        fullSectors: Map<Int, String>,
        startPage: Int = USER_START_PAGE
    ): Boolean {
        val pages = encodeUriToUltralightPages(uri, startPage) ?: return false
        val locked = TagLocks.parseLockedPages(fullSectors)
        return canWritePages(pages.keys, locked)
    }

    private fun flattenUserPages(pages: Map<Int, ByteArray>): ByteArray {
        val pageMap = TreeMap<Int, ByteArray>()
        for ((startPage, data) in pages.entries.sortedBy { it.key }) {
            val chunkCount = (data.size + 3) / 4
            for (k in 0 until chunkCount) {
                val p = startPage + k
                if (p < USER_START_PAGE) continue
                val offset = k * 4
                val len = minOf(4, data.size - offset)
                if (len <= 0) continue
                val pageBytes = ByteArray(4)
                System.arraycopy(data, offset, pageBytes, 0, len)
                pageMap[p] = pageBytes
            }
        }
        if (pageMap.isEmpty()) return ByteArray(0)
        val out = ByteArray(pageMap.size * 4)
        var idx = 0
        for ((_, pageBytes) in pageMap) {
            System.arraycopy(pageBytes, 0, out, idx, 4)
            idx += 4
        }
        return out
    }

    private fun decodeNdefUriFromTlv(raw: ByteArray): String? {
        var i = 0
        while (i < raw.size) {
            val tag = raw[i].toInt() and 0xFF
            when (tag) {
                0x00 -> i += 1 // NULL TLV
                0xFE -> return null // Terminator TLV
                0x01, 0x02 -> {
                    if (i + 1 >= raw.size) return null
                    val len = raw[i + 1].toInt() and 0xFF
                    i += 2 + len
                }
                0x03 -> {
                    if (i + 1 >= raw.size) return null
                    val firstLen = raw[i + 1].toInt() and 0xFF
                    val (ndefLen, msgStart) = if (firstLen == 0xFF) {
                        if (i + 3 >= raw.size) return null
                        val extLen = ((raw[i + 2].toInt() and 0xFF) shl 8) or (raw[i + 3].toInt() and 0xFF)
                        extLen to (i + 4)
                    } else {
                        firstLen to (i + 2)
                    }
                    if (ndefLen <= 0 || msgStart + ndefLen > raw.size) return null
                    parseNdefUriRecord(raw, msgStart, ndefLen)?.let { return it }
                    i = msgStart + ndefLen
                }
                else -> return null
            }
        }
        return null
    }

    private fun parseNdefUriRecord(raw: ByteArray, offset: Int, length: Int): String? {
        if (length < 5 || offset + length > raw.size) return null
        val header = raw[offset].toInt() and 0xFF
        val tnf = header and 0x07
        val isShortRecord = (header and 0x10) != 0
        val hasIdLength = (header and 0x08) != 0
        if (tnf != 0x01 || !isShortRecord) return null

        val typeLen = raw[offset + 1].toInt() and 0xFF
        val payloadLen = raw[offset + 2].toInt() and 0xFF
        var pos = offset + 3
        val idLen = if (hasIdLength) {
            if (pos >= offset + length) return null
            val l = raw[pos].toInt() and 0xFF
            pos += 1
            l
        } else 0

        if (typeLen != 1 || pos + typeLen + idLen + payloadLen > offset + length) return null
        val typeByte = raw[pos].toInt() and 0xFF
        pos += typeLen + idLen
        if (typeByte != 0x55 || payloadLen < 1) return null // 'U'

        val prefixCode = raw[pos].toInt() and 0xFF
        val uriBody = String(raw, pos + 1, payloadLen - 1, Charsets.UTF_8)
        val prefix = ndefUriPrefix(prefixCode)
        return prefix + uriBody
    }

    private fun decodeRawAsciiFasadUri(raw: ByteArray): String? {
        val text = String(raw, Charsets.UTF_8)
        val startIdx = text.indexOf(BASE_URI, ignoreCase = true)
        if (startIdx < 0) return null
        val sb = StringBuilder()
        for (i in startIdx until text.length) {
            val ch = text[i]
            if (ch.code <= 0x20 || ch.code == 0xFE || ch.code == 0xFFFD) break
            sb.append(ch)
        }
        return sb.toString().takeIf { it.isNotEmpty() }
    }

    private fun ndefUriPrefix(code: Int): String = when (code) {
        0x00 -> ""
        0x01 -> "http://www."
        0x02 -> "https://www."
        0x03 -> "http://"
        0x04 -> "https://"
        else -> ""
    }

    private fun normalizeUidHex(uidHex: String): String =
        uidHex.trim().uppercase().filter { it in '0'..'9' || it in 'A'..'F' }

    internal fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    internal fun urlDecode(value: String): String =
        try {
            URLDecoder.decode(value, Charsets.UTF_8.name())
        } catch (_: Exception) {
            value
        }
}
