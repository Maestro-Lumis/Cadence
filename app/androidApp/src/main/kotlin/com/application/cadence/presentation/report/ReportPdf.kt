package com.application.cadence.presentation.report

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

private fun buildReportPdf(context: Context, report: ReportUi): File {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    val margin = 48f
    var y = 64f

    paint.color = Color.BLACK
    paint.textSize = 22f
    paint.isFakeBoldText = true
    canvas.drawText(report.studentName, margin, y, paint)

    y += 26f
    paint.textSize = 12f
    paint.isFakeBoldText = false
    paint.color = Color.DKGRAY
    canvas.drawText("${report.course} · ${report.periodLabel}", margin, y, paint)

    y += 36f
    paint.color = Color.BLACK
    paint.textSize = 13f
    paint.isFakeBoldText = true
    canvas.drawText("Проведённые занятия", margin, y, paint)
    paint.isFakeBoldText = false

    y += 22f
    report.rows.forEach { row ->
        canvas.drawText(row.dateLabel, margin, y, paint)
        canvas.drawText(row.durationLabel, 360f, y, paint)
        y += 20f
    }

    if (report.rows.isEmpty()) {
        paint.color = Color.DKGRAY
        canvas.drawText("Занятий за период нет", margin, y, paint)
        paint.color = Color.BLACK
        y += 20f
    }

    y += 16f
    paint.textSize = 14f
    paint.isFakeBoldText = true
    canvas.drawText("Занятий: ${report.totalCount} · Всего: ${report.totalLabel}", margin, y, paint)

    document.finishPage(page)

    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, report.fileName)
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}

private fun uriFor(context: Context, file: File) =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

fun shareReportPdf(context: Context, report: ReportUi) {
    val uri = uriFor(context, buildReportPdf(context, report))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Отчёт"))
}

fun openReportPdf(context: Context, report: ReportUi) {
    val uri = uriFor(context, buildReportPdf(context, report))
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Нет приложения для просмотра PDF", Toast.LENGTH_SHORT).show()
    }
}
