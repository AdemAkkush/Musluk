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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var state by remember {
                mutableStateOf(WaterData())
            }
            AkilliMuslukTheme {
                DisposableEffect(key1 = true, effect = {
                    val listener = Firebase.firestore
                        .collection("veri")
                        .document("user1")
                        .addSnapshotListener { value, error ->
                            if (error != null) {
                                state = WaterData()
                            } else if (value != null && value.exists()) {
                                state = value.toObject(WaterData::class.java) ?: WaterData()
                            }
                        }
                    onDispose {
                        listener.remove()
                    }
                })

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 12.dp)) {
                                Text(text = "Su Akış Miktarı")
                                Text(text = "${state.waterFlow} L/M", fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally,modifier = Modifier.padding(end = 12.dp)) {
                                Text(text = "Su Sıcaklığı")
                                Text(text = "${state.waterTemperature} C", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 36.dp)) {
                            Text(text = "Su Açılma Sayısı")
                            Text(text = "${state.openedCount}" ,fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Column (horizontalAlignment = Alignment.CenterHorizontally){
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Su Fiyatı")
                            Text(text = "${state.price} TL", fontWeight = FontWeight.SemiBold)
                        }
                        Button(onClick = {
                            state=state.copy(price = state.unitPrice*state.totalWaterUsage)
                        }) {
                            Text(text = "Fatura Hesapla")
                        }
                    }

                }
            }
        }
    }
}

data class WaterData(
    val openedCount: Int = 0,
    val unitPrice: Float = 0f,
    val waterFlow: Float = 0f,
    val waterTemperature: Float = 0f,
    val price:Float=0f,
    val totalWaterUsage:Float=0f
)
