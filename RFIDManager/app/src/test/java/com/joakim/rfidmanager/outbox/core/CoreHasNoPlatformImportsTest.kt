package com.joakim.rfidmanager.outbox.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Skyddar kärnans portabilitet: filerna i outbox/core får bara importera kotlin.*, kotlinx.coroutines.*
 * och java.*, inte Android, Room, Paho eller appens övriga paket.
 */
class CoreHasNoPlatformImportsTest {
    @Test fun kärnan_importerar_inget_plattformsspecifikt() {
        val dir = listOf(
            File("src/main/java/com/joakim/rfidmanager/outbox/core"),
            File("app/src/main/java/com/joakim/rfidmanager/outbox/core")
        ).firstOrNull { it.isDirectory }
        requireNotNull(dir) { "hittar inte kärnans katalog från ${File(".").absolutePath}" }
        val files = dir.listFiles { f -> f.extension == "kt" }!!.toList()
        assertTrue("inga kärnfiler hittades", files.size >= 5)
        val allowed = listOf("kotlin.", "kotlinx.coroutines.", "java.")
        val offenders = files.flatMap { f ->
            f.readLines().filter { it.startsWith("import ") }
                .map { it.removePrefix("import ").trim() }
                .filter { imp -> allowed.none { imp.startsWith(it) } }
                .map { "${f.name}: $it" }
        }
        assertTrue("otillåtna importer i kärnan: $offenders", offenders.isEmpty())
    }
}
