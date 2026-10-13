package com.gorev.harcama

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Ayarlar ve devam eden form telefonda SharedPreferences içinde JSON olarak saklanır. */
class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("gorev_harcama", Context.MODE_PRIVATE)

    fun loadSettings(): Settings {
        val o = prefs.getString("settings", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return Settings()
        return Settings(
            personName = o.optString("personName"),
            recipientEmail = o.optString("recipientEmail"),
            weekdayRate = o.optDouble("weekdayRate", 1200.0),
            lateReturnRate = o.optDouble("lateReturnRate", 80.0),
            hotelRate = o.optDouble("hotelRate", 2400.0),
        )
    }

    fun saveSettings(s: Settings) {
        val o = JSONObject()
            .put("personName", s.personName)
            .put("recipientEmail", s.recipientEmail)
            .put("weekdayRate", s.weekdayRate)
            .put("lateReturnRate", s.lateReturnRate)
            .put("hotelRate", s.hotelRate)
        prefs.edit().putString("settings", o.toString()).apply()
    }

    fun loadReport(): Report {
        val o = prefs.getString("report", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return Report()
        val arr = o.optJSONArray("expenses") ?: JSONArray()
        val expenses = (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            Expense(
                id = e.optLong("id", System.nanoTime() + i),
                firm = e.optString("firm"),
                receiptNo = e.optString("receiptNo"),
                date = e.optString("date").toDate(),
                amount = e.optDouble("amount", 0.0),
                companyCard = e.optBoolean("companyCard"),
            )
        }
        return Report(
            destination = o.optString("destination"),
            startDate = o.optString("startDate").toDate(),
            endDate = o.optString("endDate").toDate(),
            weekdayDays = o.optInt("weekdayDays"),
            lateDays = o.optInt("lateDays"),
            hotelDays = o.optInt("hotelDays"),
            expenses = expenses,
        )
    }

    fun saveReport(r: Report) {
        val arr = JSONArray()
        r.expenses.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("firm", e.firm)
                    .put("receiptNo", e.receiptNo)
                    .put("date", e.date?.toString() ?: "")
                    .put("amount", e.amount)
                    .put("companyCard", e.companyCard)
            )
        }
        val o = JSONObject()
            .put("destination", r.destination)
            .put("startDate", r.startDate?.toString() ?: "")
            .put("endDate", r.endDate?.toString() ?: "")
            .put("weekdayDays", r.weekdayDays)
            .put("lateDays", r.lateDays)
            .put("hotelDays", r.hotelDays)
            .put("expenses", arr)
        prefs.edit().putString("report", o.toString()).apply()
    }

    private fun String.toDate(): LocalDate? =
        if (isEmpty()) null else runCatching { LocalDate.parse(this) }.getOrNull()
}
