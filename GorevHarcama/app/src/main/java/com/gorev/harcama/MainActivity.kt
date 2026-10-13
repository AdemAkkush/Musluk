package com.gorev.harcama

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = Storage(applicationContext)
        setContent {
            MaterialTheme { App(storage) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(storage: Storage) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(storage.loadSettings()) }
    var report by remember { mutableStateOf(storage.loadReport()) }
    var showSettings by remember { mutableStateOf(settings.recipientEmail.isBlank()) }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var confirmNew by remember { mutableStateOf(false) }

    fun updateReport(r: Report) { report = r; storage.saveReport(r) }
    fun updateSettings(s: Settings) { settings = s; storage.saveSettings(s) }

    val totals = report.totals(settings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Görev Bildirim Formu") },
                actions = {
                    IconButton(onClick = { confirmNew = true }) { Icon(Icons.Default.NoteAdd, "Yeni hafta") }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Ayarlar") }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        when {
                            settings.recipientEmail.isBlank() -> {
                                Toast.makeText(context, "Önce alıcı e-posta adresini girin", Toast.LENGTH_SHORT).show()
                                showSettings = true
                            }
                            report.startDate == null || report.endDate == null ->
                                Toast.makeText(context, "Çıkış ve dönüş tarihlerini seçin", Toast.LENGTH_SHORT).show()
                            else -> runCatching { MailSender.send(context, settings, report) }
                                .onFailure { Toast.makeText(context, "Gönderilemedi: ${it.message}", Toast.LENGTH_LONG).show() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                ) {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text("E-posta ile gönder  (${totals.grand.tl()})")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard("Görev Bilgileri") {
                    Text(settings.personName.ifBlank { "Ad soyad ayarlardan girilir" }, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = report.destination,
                        onValueChange = { updateReport(report.copy(destination = it)) },
                        label = { Text("Görevlendirilen firma ve şehirler") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateField("Görev çıkış", report.startDate, Modifier.weight(1f)) {
                            updateReport(report.copy(startDate = it).withSuggestedDays())
                        }
                        DateField("Görev dönüş", report.endDate, Modifier.weight(1f)) {
                            updateReport(report.copy(endDate = it).withSuggestedDays())
                        }
                    }
                }
            }
            item {
                SectionCard("Günlük Görev Alacakları") {
                    DaysRow("Hafta içi görev", settings.weekdayRate, report.weekdayDays, totals.weekday) {
                        updateReport(report.copy(weekdayDays = it))
                    }
                    DaysRow("Gece geç dönüş", settings.lateReturnRate, report.lateDays, totals.late) {
                        updateReport(report.copy(lateDays = it))
                    }
                    DaysRow("Otel", settings.hotelRate, report.hotelDays, totals.hotel) {
                        updateReport(report.copy(hotelDays = it))
                    }
                    HorizontalDivider()
                    TotalRow("Görev alacağı toplamı", totals.duty)
                    TotalRow("Görev harcamaları toplamı", totals.expenses)
                    TotalRow("Personelin genel alacağı", totals.grand, bold = true)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Harcama Fişleri", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = { editing = Expense(date = report.startDate) }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Fiş ekle")
                    }
                }
            }
            if (report.expenses.isEmpty()) {
                item { Text("Henüz fiş eklenmedi (yemek, ekstra harcama vb.).") }
            }
            items(report.expenses, key = { it.id }) { e ->
                Card(onClick = { editing = e }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.firm.ifBlank { "(firma yok)" }, fontWeight = FontWeight.Bold)
                            Text(
                                listOfNotNull(
                                    e.date?.fmt(),
                                    e.receiptNo.takeIf { it.isNotBlank() }?.let { "Fiş: $it" },
                                    if (e.companyCard) "Şirket kartı" else null,
                                ).joinToString("  •  "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(e.amount.tl(), fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            updateReport(report.copy(expenses = report.expenses.filterNot { it.id == e.id }))
                        }) { Icon(Icons.Default.Delete, "Sil") }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initial = settings,
            onDismiss = { showSettings = false },
            onSave = { updateSettings(it); showSettings = false },
        )
    }
    editing?.let { exp ->
        ExpenseDialog(
            initial = exp,
            onDismiss = { editing = null },
            onSave = { saved ->
                val list = report.expenses
                updateReport(
                    report.copy(
                        expenses = if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it }
                        else list + saved
                    )
                )
                editing = null
            },
        )
    }
    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text("Yeni hafta") },
            text = { Text("Mevcut form (tarihler, gün sayıları ve fişler) temizlenecek. Ayarlarınız korunur.") },
            confirmButton = {
                TextButton(onClick = { updateReport(Report()); confirmNew = false }) { Text("Temizle") }
            },
            dismissButton = { TextButton(onClick = { confirmNew = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun TotalRow(label: String, value: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else null)
        Text(value.tl(), fontWeight = if (bold) FontWeight.Bold else null)
    }
}

@Composable
private fun DaysRow(label: String, rate: Double, days: Int, total: Double, onDays: (Int) -> Unit) {
    var text by remember(days) { mutableStateOf(if (days == 0) "" else days.toString()) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text("${rate.tl()} / gün", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { v ->
                val clean = v.filter(Char::isDigit).take(3)
                text = clean
                onDays(clean.toIntOrNull() ?: 0)
            },
            label = { Text("Gün") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp),
        )
        Text(total.tl(), Modifier.width(96.dp), fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: LocalDate?, modifier: Modifier, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Default.DateRange, null)
        Spacer(Modifier.width(6.dp))
        Text(if (value == null) label else value.fmt())
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Vazgeç") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SettingsDialog(initial: Settings, onDismiss: () -> Unit, onSave: (Settings) -> Unit) {
    var name by remember { mutableStateOf(initial.personName) }
    var email by remember { mutableStateOf(initial.recipientEmail) }
    var weekday by remember { mutableStateOf(initial.weekdayRate.toString()) }
    var late by remember { mutableStateOf(initial.lateReturnRate.toString()) }
    var hotel by remember { mutableStateOf(initial.hotelRate.toString()) }
    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ayarlar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Ad soyad") }, singleLine = true)
                OutlinedTextField(
                    email, { email = it.trim() }, label = { Text("Formun gönderileceği e-posta") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                OutlinedTextField(weekday, { weekday = it }, label = { Text("Hafta içi görev ücreti (TL/gün)") }, singleLine = true, keyboardOptions = num)
                OutlinedTextField(late, { late = it }, label = { Text("Gece geç dönüş (TL/gün)") }, singleLine = true, keyboardOptions = num)
                OutlinedTextField(hotel, { hotel = it }, label = { Text("Otel alacağı (TL/gün)") }, singleLine = true, keyboardOptions = num)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Settings(
                        personName = name.trim(),
                        recipientEmail = email,
                        weekdayRate = parseAmount(weekday) ?: initial.weekdayRate,
                        lateReturnRate = parseAmount(late) ?: initial.lateReturnRate,
                        hotelRate = parseAmount(hotel) ?: initial.hotelRate,
                    )
                )
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun ExpenseDialog(initial: Expense, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    var firm by remember { mutableStateOf(initial.firm) }
    var receipt by remember { mutableStateOf(initial.receiptNo) }
    var date by remember { mutableStateOf(initial.date) }
    var amount by remember { mutableStateOf(if (initial.amount == 0.0) "" else initial.amount.toString().replace('.', ',')) }
    var card by remember { mutableStateOf(initial.companyCard) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fiş") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(firm, { firm = it }, label = { Text("Fişin alındığı firma") }, singleLine = true)
                OutlinedTextField(receipt, { receipt = it }, label = { Text("Fiş no") }, singleLine = true)
                DateField("Tarih seç", date, Modifier.fillMaxWidth()) { date = it }
                OutlinedTextField(
                    amount, { amount = it }, label = { Text("Tutar (TL)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(card, { card = it })
                    Text("Şirket kredi kartıyla ödendi")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parseAmount(amount) != null,
                onClick = {
                    onSave(initial.copy(firm = firm.trim(), receiptNo = receipt.trim(), date = date,
                        amount = parseAmount(amount) ?: 0.0, companyCard = card))
                },
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
