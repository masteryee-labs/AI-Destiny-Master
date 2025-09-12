package com.aidestinymaster.app.iching

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aidestinymaster.features.almanac.Hexagram
import com.aidestinymaster.features.almanac.IchingEngine
import com.aidestinymaster.features.almanac.HexagramCard
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun IchingDemoScreen(activity: ComponentActivity) {
    val hexState = remember { mutableStateOf<Hexagram?>(null) }
    val interpState = remember { mutableStateOf<com.aidestinymaster.features.almanac.IchingInterpretation?>(null) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("易經 Demo", style = MaterialTheme.typography.titleLarge)
        Button(onClick = {
            val now = ZonedDateTime.now(ZoneId.of("Asia/Taipei"))
            val hex = IchingEngine.castHexagramByTime(now)
            val interp = IchingEngine.interpretHexagram(hex, activity)
            hexState.value = hex
            interpState.value = interp
        }) { Text("即時起卦並顯示解釋") }
        val hex = hexState.value
        val interp = interpState.value
        if (hex != null && interp != null) {
            HexagramCard(hex, interp)
        }
    }
}
