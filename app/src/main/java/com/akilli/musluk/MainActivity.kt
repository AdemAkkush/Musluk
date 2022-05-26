package com.akilli.musluk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akilli.musluk.ui.theme.AkilliMuslukTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            var flow by remember {
                mutableStateOf(0.0)
            }

            var waterTemperature by remember {
                mutableStateOf(0.0)
            }

            var totalWaterUsage by remember {
                mutableStateOf(0.0)
            }

            var unitPrice by remember {
                mutableStateOf(0.0)
            }

            var openCount by remember {
                mutableStateOf(0)
            }

            var price by remember {
                mutableStateOf(0.0)
            }
            DisposableEffect(key1 = true, effect = {
                val flowListener = addFloatListener("flow") {
                    flow = it
                }
                val temperatureListener = addFloatListener("temperature") {
                    waterTemperature = it
                }
                val usageListener = addFloatListener("totalUsage") {
                    totalWaterUsage = it
                }
                val unitPriceListener = addFloatListener("unitPrice") {
                    unitPrice = it
                }
                val openCountListener = addFloatListener("openCount") {
                    openCount = it.toInt()
                }
                onDispose {
                    Firebase.database.reference.removeEventListener(flowListener)
                    Firebase.database.reference.removeEventListener(temperatureListener)
                    Firebase.database.reference.removeEventListener(usageListener)
                    Firebase.database.reference.removeEventListener(unitPriceListener)
                    Firebase.database.reference.removeEventListener(openCountListener)
                }
            })
            AkilliMuslukTheme {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text(text = "Su Akış Miktarı")
                                Text(text = "$flow L/M", fontWeight = FontWeight.SemiBold)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(text = "Su Sıcaklığı")
                                Text(text = "$waterTemperature C", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 36.dp)
                        ) {
                            Text(text = "Su Açılma Sayısı")
                            Text(text = "$openCount", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 36.dp)
                    ) {
                        Text(text = "Toplam Harcanan Su")
                        Text(text = "$totalWaterUsage", fontWeight = FontWeight.SemiBold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Su Fiyatı")
                            Text(text = "${price.toFloat()} TL", fontWeight = FontWeight.SemiBold)
                        }
                        Button(onClick = {
                            price = unitPrice * totalWaterUsage
                        }) {
                            Text(text = "Fatura Hesapla")
                        }
                    }

                }
            }
        }
    }

    private fun addFloatListener(value: String, onChange: (Double) -> Unit): ValueEventListener {
        return Firebase.database("https://bitirme-projesi-f45f8-default-rtdb.europe-west1.firebasedatabase.app")
            .reference
            .child(value).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println(snapshot)
                    onChange((snapshot.value as Number).toDouble())
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }
}

