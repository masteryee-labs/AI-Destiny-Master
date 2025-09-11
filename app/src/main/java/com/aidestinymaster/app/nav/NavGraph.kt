package com.aidestinymaster.app.nav

import androidx.activity.ComponentActivity
import android.content.Intent
import android.util.Log
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aidestinymaster.features.bazi.BaziDebugScreen
import com.aidestinymaster.data.prefs.UserPrefsRepository
import kotlinx.coroutines.flow.first
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidestinymaster.app.theme.ThemeViewModel
import com.aidestinymaster.data.repository.ReportRepository
import kotlinx.coroutines.launch
import android.content.pm.ApplicationInfo

object Routes {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Settings = "settings"
    const val BaziDebug = "baziDebug"
    const val ChartInput = "chartInput/{kind}"
    const val ChartResult = "chartResult/{chartId}"
    const val Report = "report/{reportId}"
    const val Paywall = "paywall"
    const val ReportFavs = "reportFavs"
}

@Composable
fun AppNav(activity: ComponentActivity, externalNav: NavHostController? = null, intent: Intent? = null) {
    val nav = externalNav ?: rememberNavController()
    val themeVm: ThemeViewModel = viewModel(viewModelStoreOwner = activity)
    val theme = themeVm.themeState.value
    val isDark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
        val start = remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            val repo = UserPrefsRepository.from(activity)
            val done = repo.onboardingDoneFlow.first()
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
                route = Routes.BaziDebug,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://bazi" })
            ) { BaziDebugScreen(activity) }
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
            // Note: debug create is handled manually below (post-graph) to avoid handleDeepLink quirks
        }
        // Handle deep links only after NavHost has set the graph to avoid NPE
        val deepLinkHandled = remember { mutableStateOf(false) }
        val latestIntent = remember { mutableStateOf(intent) }
        LaunchedEffect(intent) {
            latestIntent.value = intent
            // allow handling a new incoming intent
            deepLinkHandled.value = false
        }
        LaunchedEffect(start.value, latestIntent.value) {
            if (!deepLinkHandled.value && latestIntent.value != null && start.value != null) {
                // Mark handled early to avoid double-entry races on recomposition
                deepLinkHandled.value = true
                val i = latestIntent.value
                try {
                    val uriString = i?.dataString ?: ""
                    Log.i("AIDM", "NavGraph.handleDeepLink (post-graph) uri=" + uriString)
                    val uri = try { Uri.parse(uriString) } catch (_: Exception) { null }
                    // Manual handling for debug create
                    val isDebug = (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    if (isDebug && uri != null && uri.scheme == "aidm" && uri.host == "debug" && (uri.path ?: "").startsWith("/createReport")) {
                        val typeArg = uri.getQueryParameter("type") ?: "demo"
                        val contentArg = uri.getQueryParameter("content") ?: ("Auto content " + System.currentTimeMillis())
                        val repo = ReportRepository.from(activity)
                        val id = repo.createFromAi(typeArg, chartId = "debugChart", content = contentArg)
                        Log.i("AIDM", "CreatedReportId=" + id)
                        nav.navigate(Routes.Report.replace("{reportId}", id)) {
                            popUpTo(Routes.Home) { inclusive = false }
                        }
                    } else {
                        // Fall back to normal deep link handling
                        nav.handleDeepLink(i!!)
                    }
                } catch (t: Throwable) {
                    Log.w("AIDM", "Deep link handle failed: ${t.message}")
                }
            }
        }
    }
}

@Composable
private fun TopNavBar(nav: NavHostController) {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { nav.navigate(Routes.Home) }) { Text("Home") }
        Button(onClick = { nav.navigate(Routes.Settings) }) { Text("Settings") }
        Button(onClick = { nav.navigate(Routes.ReportFavs) }) { Text("Favs") }
        Button(onClick = { nav.navigate(Routes.BaziDebug) }) { Text("BaZi") }
    }
}
