package com.aidestinymaster.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.aidestinymaster.app.settings.SettingsScreen
import com.aidestinymaster.app.debug.DebugScreen

object Routes {
    const val Home = "home"
    const val Settings = "settings"
}

@Composable
fun AppNav(activity: ComponentActivity) {
    val nav = rememberNavController()
    MaterialTheme {
        TopNavBar(nav)
        Spacer(Modifier.height(8.dp))
        NavHost(navController = nav, startDestination = Routes.Home) {
            composable(Routes.Home) { DebugScreen(activity) }
            composable(
                route = Routes.Settings,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://settings" })
            ) { SettingsScreen(activity) }
        }
    }
}

@Composable
private fun TopNavBar(nav: NavHostController) {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { nav.navigate(Routes.Home) }) { Text("Debug") }
        Button(onClick = { nav.navigate(Routes.Settings) }) { Text("Settings") }
    }
}

