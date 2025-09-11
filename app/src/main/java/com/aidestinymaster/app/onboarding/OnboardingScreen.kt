package com.aidestinymaster.app.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.aidestinymaster.app.nav.Routes
import com.aidestinymaster.data.prefs.UserPrefsRepository

@Composable
fun OnboardingScreen(activity: ComponentActivity, nav: NavController, from: String? = null) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val prefsRepo = UserPrefsRepository.from(activity)
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("歡迎使用 AI 命理大師", style = MaterialTheme.typography.titleLarge)
        Text("本應用提供紫微、八字、占星等功能，並支援離線 AI 解讀。")
        Button(onClick = {
            scope.launch {
                prefsRepo.setOnboardingDone(true)
                val target = if (from == "settings") Routes.Settings else Routes.Home
                nav.navigate(target) { popUpTo(Routes.Onboarding) { inclusive = true } }
            }
        }) { Text("開始使用") }
    }
}
