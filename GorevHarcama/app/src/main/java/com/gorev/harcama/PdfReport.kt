package com.gorev.harcama

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/** Görev Bildirim Formu'nu (FR.048) A4 PDF olarak üretir. */
object PdfReport {
    private const val W = 595
    private const val H = 842
    private const val M = 36f

    fun create(context: Context, settings: Settings, report: Report): File {
        val doc = PdfDocument()
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = Color.BLACK }
        val bold = Paint(text).apply { typeface = Typeface.DEFAULT_BOLD }
        val line = Paint().apply { color = Color.DKGRAY; strokeWidth = 0.7f }
        val t = report.totals(settings)

        var pageNo = 0
        lateinit var page: PdfDocument.Page
        lateinit var c: android.graphics.Canvas
        var y = 0f

        fun newPage() {
            if (pageNo > 0) doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            c = page.canvas
            y = M
        }

        fun right(s: String, x: Float, p: Paint = text) {
            c.drawText(s, x - p.measureText(s), y, p)
        }

        newPage()
        c.drawText("DONANIM KALİBRASYON", M, y + 12, bold.apply { textSize = 14f })
        bold.textSize = 10f
        y += 28
        c.drawText("Görev Bildirim Formu", M, y, bold.apply { textSize = 12f })
        bold.textSize = 10f
        right("Doküman No: FR.048", W - M)
        y += 14
        right("Yayın Tarihi: 01.03.2016", W - M)
        y += 20
        c.drawLine(M, y, W - M, y, line)
        y += 18

        fun field(label: String, value: String) {
            c.drawText(label, M, y, bold)
            c.drawText(": $value", M + 190, y, text)
            y += 16
        }
        field("Görev Çıkış Tarihi", report.startDate.fmt())
        field("Görev Dönüş Tarihi", report.endDate.fmt())
        field("Görevlendirilen Personel", settings.personName)
        field("Görevlendirilen Firma ve Şehirler", report.destination)
        y += 6
        c.drawLine(M, y, W - M, y, line)
        y += 18

        c.drawText("Günlük Görev Alacakları", M, y, bold)
        y += 18
        fun calc(label: String, rate: Double, days: Int, total: Double) {
            c.drawText(label, M, y, text)
            right(rate.tl(), M + 260)
            c.drawText("X  $days gün", M + 275, y, text)
            right(total.tl(), W - M)
            y += 16
        }
        calc("Hafta İçi Görev Alacağı", settings.weekdayRate, report.weekdayDays, t.weekday)
        calc("Gece Geç Dönüş", settings.lateReturnRate, report.lateDays, t.late)
        calc("Otel Alacağı", settings.hotelRate, report.hotelDays, t.hotel)
        y += 4
        c.drawLine(M, y, W - M, y, line)
        y += 16
        c.drawText("PERSONELİN GÖREV ALACAĞI TOPLAMI", M, y, bold); right(t.duty.tl(), W - M, bold); y += 16
        c.drawText("GÖREV HARCAMALARI TOPLAMI", M, y, bold); right(t.expenses.tl(), W - M, bold); y += 16
        c.drawText("PERSONELİN GENEL ALACAK", M, y, bold); right(t.grand.tl(), W - M, bold); y += 24

        val colNo = M
        val colFirm = M + 28
        val colReceipt = M + 230
        val colDate = M + 310
        val colAmountRight = M + 450
        val colCard = M + 465

        fun header() {
            c.drawText("S.No", colNo, y, bold)
            c.drawText("Fişin Alındığı Firma", colFirm, y, bold)
            c.drawText("Fiş No", colReceipt, y, bold)
            c.drawText("Tarih", colDate, y, bold)
            right("Tutar", colAmountRight, bold)
            c.drawText("Şirket K.Kartı", colCard, y, bold)
            y += 6
            c.drawLine(M, y, W - M, y, line)
            y += 14
        }
        header()
        report.expenses.forEachIndexed { i, e ->
            if (y > H - 80) {
                newPage()
                header()
            }
            c.drawText("${i + 1}", colNo, y, text)
            c.drawText(e.firm.ellipsize(text, colReceipt - colFirm - 6), colFirm, y, text)
            c.drawText(e.receiptNo.ellipsize(text, colDate - colReceipt - 6), colReceipt, y, text)
            c.drawText(e.date.fmt(), colDate, y, text)
            right(e.amount.tl(), colAmountRight)
            c.drawText(if (e.companyCard) "Evet" else "", colCard, y, text)
            y += 15
        }
        y += 2
        c.drawLine(M, y, W - M, y, line)
        y += 16
        c.drawText("GÖREV HARCAMALARI TOPLAMI", M, y, bold)
        right(t.expenses.tl(), colAmountRight, bold)
        c.drawText("fr.048/01.03.2016-00", M, H - 24f, text.apply { textSize = 8f })

        doc.finishPage(page)
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName(settings, report))
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun fileName(settings: Settings, report: Report): String {
        val who = settings.personName.ifBlank { "Personel" }.replace(Regex("[^\\p{L}\\p{N}]+"), "_")
        val d = report.startDate?.toString() ?: "tarihsiz"
        return "Gorev_Bildirim_${who}_$d.pdf"
    }

    private fun String.ellipsize(p: Paint, maxWidth: Float): String {
        if (p.measureText(this) <= maxWidth) return this
        var s = this
        while (s.isNotEmpty() && p.measureText("$s…") > maxWidth) s = s.dropLast(1)
        return "$s…"
    }
}
