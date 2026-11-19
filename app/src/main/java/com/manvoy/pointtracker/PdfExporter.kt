package com.manvoy.pointtracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun exportToPdf(
        context: Context,
        uri: Uri,
        transactions: List<TransactionItem>
    ): Boolean {
        val pdfDocument = PdfDocument()

        val navyPaint = Paint().apply {
            color = Color.parseColor("#002D5E")
            style = Paint.Style.FILL
        }
        val goldPaint = Paint().apply {
            color = Color.parseColor("#F4B400")
            style = Paint.Style.FILL
        }
        val coralPaint = Paint().apply {
            color = Color.parseColor("#E87A5D")
            style = Paint.Style.FILL
        }
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F3F6FA")
            style = Paint.Style.FILL
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#D8E0EA")
            strokeWidth = 1f
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D6E4F5")
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A6878")
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#002D5E")
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bonusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B7A43")
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D2733")
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val pointCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B55336")
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val summary = calculatePointsSummary(transactions)
        val printDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

        val colNo = MARGIN + 6f
        val colDate = MARGIN + 30f
        val colName = MARGIN + 108f
        val colType = MARGIN + 292f
        val colAmount = MARGIN + 365f
        val colPoints = MARGIN + 468f

        fun drawTableHeader(canvas: Canvas, topY: Float): Float {
            canvas.drawRect(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + 24f, navyPaint)
            val textY = topY + 15.5f
            canvas.drawText("No", colNo, textY, headerTextPaint)
            canvas.drawText("Tanggal", colDate, textY, headerTextPaint)
            canvas.drawText("Nama Transaksi", colName, textY, headerTextPaint)
            canvas.drawText("Jenis", colType, textY, headerTextPaint)
            canvas.drawText("Nominal", colAmount, textY, headerTextPaint)
            canvas.drawText("Poin", colPoints, textY, headerTextPaint)
            return topY + 24f
        }

        fun truncateText(text: String, maxChars: Int): String {
            return if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"
        }

        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Header Banner
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 84f, navyPaint)
            canvas.drawRect(0f, 84f, PAGE_WIDTH * 0.65f, 89f, goldPaint)
            canvas.drawRect(PAGE_WIDTH * 0.65f, 84f, PAGE_WIDTH.toFloat(), 89f, coralPaint)

            canvas.drawText("LAPORAN TRACKER POIN MANVOY", MARGIN, 40f, titlePaint)
            canvas.drawText("Dicetak pada: $printDate WIB", MARGIN, 60f, subtitlePaint)

            // Summary Box (2 baris metrik)
            val summaryRect = RectF(MARGIN, 104f, PAGE_WIDTH - MARGIN, 212f)
            canvas.drawRoundRect(summaryRect, 8f, 8f, cardBgPaint)

            val col1 = MARGIN + 16f
            val col2 = MARGIN + 184f
            val col3 = MARGIN + 364f

            // Baris 1: Transaksi, Nominal, Total Poin
            canvas.drawText("Total Transaksi", col1, 124f, labelPaint)
            canvas.drawText("${summary.transactionCount} Transaksi", col1, 144f, valuePaint)

            canvas.drawText("Total Nominal", col2, 124f, labelPaint)
            canvas.drawText(formatRupiah(summary.totalAmount), col2, 144f, valuePaint)

            canvas.drawText("Total Poin (Dasar + Bonus)", col3, 124f, labelPaint)
            canvas.drawText("${formatNumber(summary.totalPoints)} Poin", col3, 144f, valuePaint)

            // Garis pemisah tipis dalam summary box
            canvas.drawLine(MARGIN + 14f, 158f, PAGE_WIDTH - MARGIN - 14f, 158f, dividerPaint)

            // Baris 2: Bonus 30jt, Earning Rate Rp/Poin, Rata-rata Poin/20rb
            canvas.drawText("Bonus Target Rp30 Jt", col1, 176f, labelPaint)
            if (summary.isBonusUnlocked) {
                canvas.drawText("+${formatNumber(summary.bonusPoints)} Poin (Tercapai)", col1, 196f, bonusPaint)
            } else {
                canvas.drawText("0 Poin (Kurang ${formatRupiah(summary.remainingForBonus)})", col1, 196f, cellTextPaint)
            }

            canvas.drawText("Earning Rate (Rp/Poin)", col2, 176f, labelPaint)
            canvas.drawText(formatRupiahPerPoint(summary.rupiahPerPoint), col2, 196f, valuePaint)

            canvas.drawText("Rata-rata per Rp20rb", col3, 176f, labelPaint)
            canvas.drawText(formatPointsPer20k(summary.pointsPer20k), col3, 196f, valuePaint)

            var currentY = drawTableHeader(canvas, 228f)

            if (transactions.isEmpty()) {
                canvas.drawText(
                    "Belum ada data transaksi tercatat.",
                    MARGIN + 8f,
                    currentY + 24f,
                    cellTextPaint
                )
            } else {
                transactions.forEachIndexed { index, item ->
                    if (currentY + 24f > PAGE_HEIGHT - MARGIN) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = drawTableHeader(canvas, MARGIN)
                    }

                    if (index % 2 == 1) {
                        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f, cardBgPaint)
                    }

                    val textY = currentY + 14.5f
                    canvas.drawText("${index + 1}", colNo, textY, cellTextPaint)
                    canvas.drawText(formatDateUtc(item.dateMillis), colDate, textY, cellTextPaint)
                    canvas.drawText(truncateText(item.name, 30), colName, textY, cellTextPaint)
                    canvas.drawText(item.type.label, colType, textY, cellTextPaint)
                    canvas.drawText(formatRupiah(item.amount), colAmount, textY, cellTextPaint)
                    canvas.drawText("+${formatNumber(item.points)}", colPoints, textY, pointCellPaint)

                    currentY += 22f
                    canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, dividerPaint)
                }
            }

            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            } ?: return false

            return true
        } catch (_: Exception) {
            return false
        } finally {
            pdfDocument.close()
        }
    }
}
