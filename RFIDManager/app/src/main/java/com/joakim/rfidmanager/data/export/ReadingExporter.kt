package com.joakim.rfidmanager.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.joakim.rfidmanager.domain.model.PersistedReading
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ReadingExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportToCsv(readings: List<PersistedReading>): String {
        val sb = StringBuilder()
        sb.appendLine("UID,Type,Timestamp,Source,DataPreview,Transmitted,MemoryBank,Address,Length,Payload")
        for (r in readings) {
            val timeStr = dateFormat.format(Date(r.timestamp))
            sb.appendLine("${r.uidOrCode},${r.type},$timeStr,${r.source ?: ""},${r.dataPreview ?: ""},${r.transmitted},${r.memoryBank ?: ""},${r.address ?: ""},${r.length ?: ""},${r.payload ?: ""}")
        }
        return sb.toString()
    }

    fun exportToJson(readings: List<PersistedReading>): String {
        val items = readings.joinToString(",\n") { r ->
            val timeStr = dateFormat.format(Date(r.timestamp))
            """    {
      "uid": "${r.uidOrCode}",
      "type": "${r.type}",
      "timestamp": "$timeStr",
      "source": ${jsonStr(r.source)},
      "dataPreview": ${jsonStr(r.dataPreview)},
      "transmitted": ${r.transmitted},
      "memoryBank": ${r.memoryBank ?: "null"},
      "address": ${r.address ?: "null"},
      "length": ${r.length ?: "null"},
      "payload": ${jsonStr(r.payload)}
    }"""
        }
        return "[\n$items\n]\n"
    }

    fun shareFile(context: Context, readings: List<PersistedReading>, format: String) {
        val content = if (format == "csv") exportToCsv(readings) else exportToJson(readings)
        val ext = if (format == "csv") "csv" else "json"
        val mime = if (format == "csv") "text/csv" else "application/json"

        val dir = File(context.cacheDir, "exports")
        dir.mkdirs()
        val file = File(dir, "readings.$ext")
        file.writeText(content)

        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export readings"))
    }

    private fun jsonStr(s: String?): String =
        if (s == null) "null" else "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
