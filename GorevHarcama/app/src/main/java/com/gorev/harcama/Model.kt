package com.gorev.harcama

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Formdaki "Fişin Alındığı Firma / Fiş No / Tarih / Tutar / Şirket K.Kartı" satırı. */
data class Expense(
    val id: Long = System.nanoTime(),
    val firm: String = "",
    val receiptNo: String = "",
    val date: LocalDate? = null,
    val amount: Double = 0.0,
    val companyCard: Boolean = false,
)

/** Kişiye ve şirkete ait, haftadan haftaya değişmeyen ayarlar. */
data class Settings(
    val personName: String = "",
    val recipientEmail: String = "",
    val weekdayRate: Double = 1200.0,
    val lateReturnRate: Double = 80.0,
    val hotelRate: Double = 2400.0,
)

/** Bir haftalık Görev Bildirim Formu (FR.048). */
data class Report(
    val destination: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val weekdayDays: Int = 0,
    val lateDays: Int = 0,
    val hotelDays: Int = 0,
    val expenses: List<Expense> = emptyList(),
)

data class Totals(
    val weekday: Double,
    val late: Double,
    val hotel: Double,
    val expenses: Double,
) {
    /** "Personelin görev alacağı toplamı" */
    val duty get() = weekday + hotel
    /** "Personelin genel alacak" (gece geç dönüş dahil) */
    val grand get() = weekday + late + hotel + expenses
}

fun Report.totals(s: Settings) = Totals(
    weekday = s.weekdayRate * weekdayDays,
    late = s.lateReturnRate * lateDays,
    hotel = s.hotelRate * hotelDays,
    expenses = expenses.sumOf { it.amount },
)

/** Tarih aralığındaki hafta içi gün sayısı ve otel gecesi (gün sayısı - 1) önerisi. */
fun Report.withSuggestedDays(): Report {
    val s = startDate ?: return this
    val e = endDate ?: return this
    if (e.isBefore(s)) return this
    val total = ChronoUnit.DAYS.between(s, e).toInt() + 1
    val weekdays = (0 until total).count {
        val d = s.plusDays(it.toLong()).dayOfWeek
        d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY
    }
    return copy(weekdayDays = weekdays, hotelDays = (total - 1).coerceAtLeast(0))
}

fun LocalDate?.fmt(): String = this?.format(DATE_FMT) ?: ""

fun Double.tl(): String = String.format(Locale("tr", "TR"), "%,.2f TL", this)

/** "1.234,50", "1234.5" gibi girişleri sayıya çevirir. */
fun parseAmount(text: String): Double? {
    val t = text.trim().replace(" ", "")
    if (t.isEmpty()) return null
    val normalized = when {
        t.contains(',') -> t.replace(".", "").replace(',', '.')
        else -> t
    }
    return normalized.toDoubleOrNull()
}
