package com.gorev.harcama

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider

object MailSender {
    /** Excel dosyasını oluşturur ve kayıtlı adrese gönderilmek üzere e-posta uygulamasını açar. */
    fun send(context: Context, settings: Settings, report: Report) {
        val xlsx = XlsxReport.create(context, settings, report)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", xlsx)
        val t = report.totals(settings)

        val subject = "Görev Bildirim Formu - ${settings.personName} - " +
            "${report.startDate.fmt()} / ${report.endDate.fmt()}"
        val body = buildString {
            appendLine("Görevlendirilen: ${settings.personName}")
            appendLine("Görev yeri: ${report.destination}")
            appendLine("Tarih: ${report.startDate.fmt()} - ${report.endDate.fmt()}")
            appendLine()
            appendLine("Görev alacağı: ${t.duty.tl()}")
            appendLine("Gece geç dönüş: ${t.late.tl()}")
            appendLine("Görev harcamaları: ${t.expenses.tl()}")
            appendLine("Genel alacak: ${t.grand.tl()}")
            appendLine()
            appendLine("Detaylı form ekte (Excel) yer almaktadır.")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = XlsxReport.MIME
            putExtra(Intent.EXTRA_EMAIL, arrayOf(settings.recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "E-posta ile gönder"))
    }
}
