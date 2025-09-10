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
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.aidestinymaster.app.settings.SettingsScreen
import com.aidestinymaster.app.debug.DebugScreen
import com.aidestinymaster.app.home.HomeScreen
import com.aidestinymaster.app.report.ReportScreen
import com.aidestinymaster.app.chart.ChartInputScreen
import com.aidestinymaster.app.chart.ChartResultScreen
import com.aidestinymaster.app.paywall.PaywallScreen
import com.aidestinymaster.app.onboarding.OnboardingScreen
import com.aidestinymaster.app.prefs.UserPrefs
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

object Routes {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Settings = "settings"
    const val ChartInput = "chartInput/{kind}"
    const val ChartResult = "chartResult/{chartId}"
    const val Report = "report/{reportId}"
    const val Paywall = "paywall"
    const val ReportFavs = "reportFavs"
}

@Composable
fun AppNav(activity: ComponentActivity) {
    val nav = rememberNavController()
    MaterialTheme {
        val start = remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            val done = UserPrefs.onboardingDoneFlow(activity).first()
            start.value = if (done) Routes.Home else Routes.Onboarding
        }
        if (start.value == null) {
            Text("Loading...")
            return@MaterialTheme
        }
        TopNavBar(nav)
        Spacer(Modifier.height(8.dp))
        NavHost(navController = nav, startDestination = start.value!!) {
            composable(
                route = Routes.Onboarding + "?from={from}",
                arguments = listOf(navArgument("from") { nullable = true }),
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://onboarding?from={from}" })
            ) { backStack ->
                val from = backStack.arguments?.getString("from")
                OnboardingScreen(activity, nav, from)
            }
            composable(Routes.Home) { HomeScreen(activity, nav) }
            composable(
                route = Routes.Settings,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://settings" })
            ) { SettingsScreen(activity) }
            composable(
                route = Routes.ChartInput,
                arguments = listOf(navArgument("kind") {}),
            ) { backStack -> ChartInputScreen(activity, backStack.arguments?.getString("kind") ?: "", nav) }
            composable(
                route = Routes.ChartResult,
                arguments = listOf(navArgument("chartId") {}),
            ) { backStack -> ChartResultScreen(activity, backStack.arguments?.getString("chartId") ?: "") }
            composable(
                route = Routes.Report,
                arguments = listOf(navArgument("reportId") {}),
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://report/{reportId}" })
            ) { backStack -> ReportScreen(activity, backStack.arguments?.getString("reportId") ?: "") }
            composable(
                route = Routes.Paywall,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://paywall" })
            ) { PaywallScreen(activity) }
            composable(Routes.ReportFavs) { com.aidestinymaster.app.report.ReportFavoritesScreen(activity, nav) }
        }
    }
}

@Composable
private fun TopNavBar(nav: NavHostController) {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { nav.navigate(Routes.Home) }) { Text("Home") }
        Button(onClick = { nav.navigate(Routes.Settings) }) { Text("Settings") }
        Button(onClick = { nav.navigate(Routes.ReportFavs) }) { Text("Favs") }
    }
}
