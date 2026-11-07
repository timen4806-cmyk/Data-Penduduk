package com.example.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.Penduduk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat {
    EXCEL,
    PDF
}

enum class StorageDestination {
    INTERNAL,
    EXTERNAL_DOWNLOADS
}

data class ExportFileResult(
    val file: File?,
    val uri: Uri,
    val format: ExportFormat,
    val destination: StorageDestination,
    val filename: String
)

class ExportRepository(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    /**
     * Exports resident list to either EXCEL (.csv) or PDF, saving to internal or external storage.
     */
    suspend fun exportData(
        residents: List<Penduduk>,
        format: ExportFormat,
        destination: StorageDestination
    ): Result<ExportFileResult> = withContext(Dispatchers.IO) {
        try {
            val timestamp = dateFormat.format(Date())
            val extension = if (format == ExportFormat.EXCEL) "csv" else "pdf"
            val filename = "Data_Penduduk_${timestamp}.$extension"

            val fileResult = when (format) {
                ExportFormat.EXCEL -> generateExcelFile(residents, filename, destination)
                ExportFormat.PDF -> generatePdfFile(residents, filename, destination)
            }

            Result.success(fileResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateExcelFile(
        residents: List<Penduduk>,
        filename: String,
        destination: StorageDestination
    ): ExportFileResult {
        val outputStream: OutputStream
        var targetFile: File? = null
        val targetUri: Uri

        val content = buildExcelCsvContent(residents)

        if (destination == StorageDestination.EXTERNAL_DOWNLOADS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DataWarga")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Gagal membuat file di folder Downloads.")
            outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Gagal membuka stream penulisan di Downloads.")
            targetUri = uri
        } else {
            val dir = if (destination == StorageDestination.INTERNAL) {
                File(context.filesDir, "exports").apply { mkdirs() }
            } else {
                @Suppress("DEPRECATION")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DataWarga").apply { mkdirs() }
            }
            val file = File(dir, filename)
            targetFile = file
            outputStream = FileOutputStream(file)
            targetUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        outputStream.use { stream ->
            // Write UTF-8 BOM so Excel opens with proper encoding
            stream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
        }

        return ExportFileResult(
            file = targetFile,
            uri = targetUri,
            format = ExportFormat.EXCEL,
            destination = destination,
            filename = filename
        )
    }

    private fun buildExcelCsvContent(residents: List<Penduduk>): String {
        val sb = StringBuilder()
        // Header
        val headers = listOf(
            "No",
            "NIK",
            "Nama Lengkap",
            "Tempat Lahir",
            "Tanggal Lahir",
            "Jenis Kelamin",
            "Alamat Lengkap",
            "RT",
            "RW",
            "Kelurahan/Desa",
            "Kecamatan",
            "Agama",
            "Status Perkawinan",
            "Pekerjaan",
            "Kewarganegaraan"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        residents.forEachIndexed { index, r ->
            val row = listOf(
                (index + 1).toString(),
                // Prepend ="..." so Excel preserves 16 digits text format without scientific notation
                "=\"${r.nik}\"",
                r.nama,
                r.tempatLahir,
                r.tanggalLahir,
                r.jenisKelamin,
                r.getFullAddress(),
                r.rt,
                r.rw,
                r.kelurahan,
                r.kecamatan,
                r.agama,
                r.statusPerkawinan,
                r.pekerjaan,
                r.kewarganegaraan
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(text: String): String {
        val needQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")
        val escaped = text.replace("\"", "\"\"")
        return if (needQuotes) "\"$escaped\"" else escaped
    }

    private fun generatePdfFile(
        residents: List<Penduduk>,
        filename: String,
        destination: StorageDestination
    ): ExportFileResult {
        val pdfDocument = PdfDocument()

        // Landscape A4 dimensions in PostScript points: 842 x 595
        val pageWidth = 842
        val pageHeight = 595
        val margin = 36f

        val headerPaint = Paint().apply {
            color = Color.rgb(27, 59, 111) // Navy
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(220, 230, 242)
            textSize = 9f
            isAntiAlias = true
        }

        val tableHeaderBg = Paint().apply {
            color = Color.rgb(35, 134, 91) // Civic Green
            isAntiAlias = true
        }

        val tableHeaderTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 7.5f
            isAntiAlias = true
        }

        val zebraBgPaint = Paint().apply {
            color = Color.rgb(244, 248, 245)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(215, 222, 230)
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            isAntiAlias = true
        }

        val rowHeight = 22f
        val rowsPerPage = 17
        val totalPages = maxOf(1, (residents.size + rowsPerPage - 1) / rowsPerPage)

        // Column widths definition for 842pt landscape width (Available width = 770pt)
        // No: 26, NIK: 90, Nama: 110, JK: 50, Lahir: 80, Alamat: 180, Agama: 55, Status: 65, Pekerjaan: 75, WN: 39 = 770
        val colWidths = floatArrayOf(26f, 92f, 112f, 50f, 82f, 176f, 55f, 65f, 74f, 38f)
        val colHeaders = arrayOf(
            "No", "NIK", "Nama Lengkap", "L/P", "Tempat/Tgl Lahir",
            "Alamat Lengkap", "Agama", "Status", "Pekerjaan", "WN"
        )

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Header Banner
            val bannerRect = RectF(margin, margin, pageWidth - margin, margin + 46f)
            canvas.drawRoundRect(bannerRect, 6f, 6f, headerPaint)
            canvas.drawText("DATAWARGA — DAFTAR ADMINISTRASI PENDUDUK", margin + 14f, margin + 22f, titlePaint)
            val printDate = displayDateFormat.format(Date())
            canvas.drawText("Dicetak pada: $printDate | Total Penduduk: ${residents.size} jiwa | Keamanan: Terenkripsi AES-256", margin + 14f, margin + 38f, subtitlePaint)

            // Table Header Bar
            var currentY = margin + 56f
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 20f, tableHeaderBg)

            var colX = margin
            for (c in colHeaders.indices) {
                val headerText = colHeaders[c]
                canvas.drawText(headerText, colX + 4f, currentY + 13.5f, tableHeaderTextPaint)
                colX += colWidths[c]
            }

            currentY += 20f

            // Rows for this page
            val startIdx = pageIndex * rowsPerPage
            val endIdx = minOf(startIdx + rowsPerPage, residents.size)

            for (i in startIdx until endIdx) {
                val r = residents[i]
                val isEven = (i - startIdx) % 2 == 1

                if (isEven) {
                    canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, zebraBgPaint)
                }

                // Row bottom border
                canvas.drawLine(margin, currentY + rowHeight, pageWidth - margin, currentY + rowHeight, borderPaint)

                val birthInfo = if (r.tempatLahir.isNotBlank() && r.tanggalLahir.isNotBlank()) {
                    "${r.tempatLahir}, ${r.tanggalLahir}"
                } else {
                    r.tempatLahir.ifBlank { r.tanggalLahir }
                }

                val rowData = arrayOf(
                    (i + 1).toString(),
                    r.nik,
                    truncate(r.nama, 22),
                    if (r.jenisKelamin.startsWith("L", ignoreCase = true)) "L" else if (r.jenisKelamin.startsWith("P", ignoreCase = true)) "P" else r.jenisKelamin,
                    truncate(birthInfo, 18),
                    truncate(r.getFullAddress(), 36),
                    truncate(r.agama, 10),
                    truncate(r.statusPerkawinan, 12),
                    truncate(r.pekerjaan, 14),
                    r.kewarganegaraan
                )

                var cellX = margin
                for (c in rowData.indices) {
                    canvas.drawText(rowData[c], cellX + 4f, currentY + 14f, textPaint)
                    cellX += colWidths[c]
                }

                currentY += rowHeight
            }

            // Outer border around table
            canvas.drawRect(margin, margin + 56f, pageWidth - margin, currentY, borderPaint)

            // Footer
            val footerText = "Halaman ${pageIndex + 1} dari $totalPages — DataWarga Sistem Administrasi Kependudukan"
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 8f
                isAntiAlias = true
            }
            canvas.drawText(footerText, margin, pageHeight - margin + 12f, footerPaint)

            pdfDocument.finishPage(page)
        }

        // Save PDF file
        val outputStream: OutputStream
        var targetFile: File? = null
        val targetUri: Uri

        if (destination == StorageDestination.EXTERNAL_DOWNLOADS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DataWarga")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Gagal membuat file PDF di folder Downloads.")
            outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Gagal membuka stream penulisan PDF di Downloads.")
            targetUri = uri
        } else {
            val dir = if (destination == StorageDestination.INTERNAL) {
                File(context.filesDir, "exports").apply { mkdirs() }
            } else {
                @Suppress("DEPRECATION")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DataWarga").apply { mkdirs() }
            }
            val file = File(dir, filename)
            targetFile = file
            outputStream = FileOutputStream(file)
            targetUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        outputStream.use { stream ->
            pdfDocument.writeTo(stream)
            stream.flush()
        }
        pdfDocument.close()

        return ExportFileResult(
            file = targetFile,
            uri = targetUri,
            format = ExportFormat.PDF,
            destination = destination,
            filename = filename
        )
    }

    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.take(maxLength - 2) + ".."
        } else {
            text
        }
    }

    /**
     * Creates an intent to view/open the exported file.
     */
    fun createViewIntent(result: ExportFileResult): Intent {
        val mimeType = if (result.format == ExportFormat.EXCEL) "text/csv" else "application/pdf"
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(result.uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an intent to share the exported file.
     */
    fun createShareIntent(result: ExportFileResult): Intent {
        val mimeType = if (result.format == ExportFormat.EXCEL) "text/csv" else "application/pdf"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, result.uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ekspor ${result.filename}")
            putExtra(Intent.EXTRA_TEXT, "Berikut adalah file ekspor data kependudukan dari DataWarga: ${result.filename}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Bagikan Berkas DataWarga").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
