package com.gorev.harcama

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.TreeMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Görev Bildirim Formu'nu (FR.048) .xlsx olarak üretir. Ek kütüphane gerekmez:
 * xlsx, XML dosyalarından oluşan bir zip'tir. Toplamlar Excel formülü olarak yazılır.
 */
object XlsxReport {
    const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    // styles.xml içindeki cellXfs sırası
    private const val DEFAULT = 0
    private const val BOLD = 1
    private const val TITLE = 2
    private const val DATE = 3
    private const val MONEY = 4
    private const val TEXT = 5
    private const val HEADER = 6
    private const val MONEY_BOLD = 7
    private const val INT = 8
    private const val LABEL = 9

    private const val MIN_EXPENSE_ROWS = 21
    private val EPOCH = LocalDate.of(1899, 12, 30)

    private class Sheet {
        val rows = TreeMap<Int, MutableList<String>>()
        val merges = mutableListOf<String>()

        private fun add(row: Int, xml: String) { rows.getOrPut(row) { mutableListOf() }.add(xml) }

        fun text(col: Char, row: Int, value: String, style: Int = DEFAULT) =
            add(row, """<c r="$col$row" s="$style" t="inlineStr"><is><t xml:space="preserve">${esc(value)}</t></is></c>""")

        fun num(col: Char, row: Int, value: Double, style: Int, formula: String? = null) {
            val f = formula?.let { "<f>${esc(it)}</f>" } ?: ""
            add(row, """<c r="$col$row" s="$style">$f<v>$value</v></c>""")
        }

        fun date(col: Char, row: Int, value: LocalDate?, style: Int = DATE) =
            if (value == null) empty(col, row, style)
            else num(col, row, ChronoUnit.DAYS.between(EPOCH, value).toDouble(), style)

        fun empty(col: Char, row: Int, style: Int) = add(row, """<c r="$col$row" s="$style"/>""")

        fun xml(): String = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<cols><col min="1" max="1" width="30" customWidth="1"/><col min="2" max="2" width="30" customWidth="1"/>""")
            append("""<col min="3" max="3" width="14" customWidth="1"/><col min="4" max="4" width="14" customWidth="1"/>""")
            append("""<col min="5" max="5" width="18" customWidth="1"/><col min="6" max="6" width="16" customWidth="1"/></cols>""")
            append("<sheetData>")
            rows.forEach { (r, cells) -> append("""<row r="$r">""").append(cells.joinToString("")).append("</row>") }
            append("</sheetData>")
            if (merges.isNotEmpty()) {
                append("""<mergeCells count="${merges.size}">""")
                merges.forEach { append("""<mergeCell ref="$it"/>""") }
                append("</mergeCells>")
            }
            append("""<pageSetup paperSize="9" orientation="portrait" fitToHeight="0"/>""")
            append("</worksheet>")
        }
    }

    fun create(context: Context, settings: Settings, report: Report): File {
        val t = report.totals(settings)
        val s = Sheet()

        s.text('A', 1, "DONANIM KALİBRASYON - Görev Bildirim Formu", TITLE)
        s.merges += "A1:F1"
        s.text('A', 2, "Doküman No: FR.048   Yayın Tarihi: 01.03.2016")
        s.merges += "A2:F2"

        s.text('A', 3, "Görev Çıkış Tarihi :", BOLD); s.date('B', 3, report.startDate)
        s.text('A', 4, "Görev Dönüş Tarihi :", BOLD); s.date('B', 4, report.endDate)
        s.text('A', 5, "Görevlendirilen Personelin Adı Soyadı :", BOLD); s.text('B', 5, settings.personName)
        s.text('A', 6, "Görevlendirilen Firma ve Şehirler :", BOLD); s.text('B', 6, report.destination)

        s.text('A', 8, "Günlük Görev Alacakları", HEADER)
        s.text('B', 8, "Görev Ücreti", HEADER)
        s.text('C', 8, "", HEADER)
        s.text('D', 8, "Görevli Gün Sayısı", HEADER)
        s.text('E', 8, "Toplam", HEADER)

        fun line(row: Int, label: String, rate: Double, days: Int, total: Double) {
            s.text('A', row, label, LABEL)
            s.num('B', row, rate, MONEY)
            s.text('C', row, "X", TEXT)
            s.num('D', row, days.toDouble(), INT)
            s.num('E', row, total, MONEY, "B$row*D$row")
        }
        line(9, "Hafta İçi Görev Alacağı", settings.weekdayRate, report.weekdayDays, t.weekday)
        line(10, "Gece Geç Dönüş", settings.lateReturnRate, report.lateDays, t.late)
        line(11, "Otel Alacağı", settings.hotelRate, report.hotelDays, t.hotel)

        val first = 17
        val rowCount = maxOf(MIN_EXPENSE_ROWS, report.expenses.size)
        val last = first + rowCount - 1
        val totalRow = last + 1

        s.text('A', 12, "PERSONELİN GÖREV ALACAĞI TOPLAMI :", LABEL)
        s.num('E', 12, t.duty, MONEY_BOLD, "E9+E11")
        s.text('A', 13, "GÖREV HARCAMALARI TOPLAMI :", LABEL)
        s.num('E', 13, t.expenses, MONEY_BOLD, "E$totalRow")
        s.text('A', 14, "PERSONELİN GENEL ALACAK :", LABEL)
        s.num('E', 14, t.grand, MONEY_BOLD, "E9+E10+E11+E$totalRow")

        s.text('A', 16, "S. No", HEADER)
        s.text('B', 16, "Fişin Alındığı Firma", HEADER)
        s.text('C', 16, "Fiş No", HEADER)
        s.text('D', 16, "Tarih", HEADER)
        s.text('E', 16, "Tutar", HEADER)
        s.text('F', 16, "Şirket K.Kartı", HEADER)

        for (i in 0 until rowCount) {
            val row = first + i
            val e = report.expenses.getOrNull(i)
            s.num('A', row, (i + 1).toDouble(), INT)
            s.text('B', row, e?.firm ?: "", TEXT)
            s.text('C', row, e?.receiptNo ?: "", TEXT)
            s.date('D', row, e?.date)
            if (e != null) s.num('E', row, e.amount, MONEY) else s.empty('E', row, MONEY)
            s.text('F', row, if (e?.companyCard == true) "Evet" else "", TEXT)
        }
        s.text('A', totalRow, "GÖREV HARCAMALARI TOPLAMI :", LABEL)
        s.num('E', totalRow, t.expenses, MONEY_BOLD, "SUM(E$first:E$last)")
        s.text('A', totalRow + 2, "fr.048/01.03.2016-00")

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName(settings, report))
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", CONTENT_TYPES)
            entry("_rels/.rels", ROOT_RELS)
            entry("xl/workbook.xml", WORKBOOK)
            entry("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            entry("xl/styles.xml", STYLES)
            entry("xl/worksheets/sheet1.xml", s.xml())
        }
        return file
    }

    fun fileName(settings: Settings, report: Report): String {
        val who = settings.personName.ifBlank { "Personel" }.replace(Regex("[^\\p{L}\\p{N}]+"), "_")
        val d = report.startDate?.toString() ?: "tarihsiz"
        return "Gorev_Bildirim_${who}_$d.xlsx"
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private const val HEAD = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""

    private const val CONTENT_TYPES = HEAD +
        """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
        """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
        """<Default Extension="xml" ContentType="application/xml"/>""" +
        """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
        """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
        """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
        "</Types>"

    private const val ROOT_RELS = HEAD +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
        "</Relationships>"

    private const val WORKBOOK = HEAD +
        """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
        """<sheets><sheet name="Görev Bildirim Formu" sheetId="1" r:id="rId1"/></sheets>""" +
        """<calcPr fullCalcOnLoad="1"/></workbook>"""

    private const val WORKBOOK_RELS = HEAD +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
        """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
        "</Relationships>"

    private const val STYLES = HEAD +
        """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
        """<numFmts count="2"><numFmt numFmtId="164" formatCode="dd\.mm\.yyyy"/>""" +
        """<numFmt numFmtId="165" formatCode="#,##0.00&quot; TL&quot;"/></numFmts>""" +
        """<fonts count="3"><font><sz val="11"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="11"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="14"/><name val="Calibri"/></font></fonts>""" +
        """<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFD9D9D9"/><bgColor indexed="64"/></patternFill></fill></fills>""" +
        """<borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border>""" +
        """<border><left style="thin"><color auto="1"/></left><right style="thin"><color auto="1"/></right>""" +
        """<top style="thin"><color auto="1"/></top><bottom style="thin"><color auto="1"/></bottom><diagonal/></border></borders>""" +
        """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
        """<cellXfs count="10">""" +
        """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
        """<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
        """<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>""" +
        """<xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>""" +
        """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>""" +
        """<xf numFmtId="165" fontId="1" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1"/>""" +
        """<xf numFmtId="1" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>""" +
        "</cellXfs></styleSheet>"
}
