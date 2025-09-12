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
import com.aidestinymaster.app.design.DesignDemoScreen
import com.aidestinymaster.app.mixai.MixAiDemoScreen
import com.aidestinymaster.app.ziwei.ZiweiDemoScreen
import com.aidestinymaster.app.iching.IchingDemoScreen
import com.aidestinymaster.features.astrochart.NatalChartScreen
import com.aidestinymaster.core.astro.AstroCalculator
import com.aidestinymaster.core.ai.ModelInstaller
import com.aidestinymaster.data.prefs.UserPrefsRepository
import kotlinx.coroutines.flow.first
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidestinymaster.app.theme.ThemeViewModel
import com.aidestinymaster.app.theme.AppTheme
import com.aidestinymaster.data.repository.ReportRepository
import kotlinx.coroutines.launch
import android.content.pm.ApplicationInfo

object Routes {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Settings = "settings"
    const val BaziDebug = "baziDebug"
    const val DesignDemo = "designDemo"
    const val MixAiDemo = "mixAiDemo"
    const val ZiweiDemo = "ziweiDemo"
    const val IchingDemo = "ichingDemo"
    const val AstroDemo = "astroDemo"
    const val ChartInput = "chartInput/{kind}"
    const val ChartResult = "chartResult/{chartId}"
    const val Report = "report/{reportId}"
    const val Paywall = "paywall"
    const val ReportFavs = "reportFavs"
}

@Composable
fun AppNav(activity: ComponentActivity, externalNav: NavHostController? = null, intent: Intent? = null) {
    val nav = externalNav ?: rememberNavController()
    // 安裝模型（如 assets/models.zip 存在則解壓至 files/models），缺檔時安全略過
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { ModelInstaller.installIfNeeded(activity, expectedSha256 = "") }
            .onSuccess { ok -> Log.d("AIDM", "Model install check: $ok") }
            .onFailure { t -> Log.w("AIDM", "Model install failed: ${t.message}") }
    }
    val themeVm: ThemeViewModel = viewModel(viewModelStoreOwner = activity)
    val theme = themeVm.themeState.value
    val isDark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    AppTheme(darkTheme = isDark) {
        val start = remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            val repo = UserPrefsRepository.from(activity)
            val done = repo.onboardingDoneFlow.first()
            start.value = if (done) Routes.Home else Routes.Onboarding
        }
        if (start.value == null) {
            Text("Loading...")
            return@AppTheme
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
            composable(
                route = Routes.Home,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://home" })
            ) { HomeScreen(activity, nav) }
            composable(
                route = Routes.BaziDebug,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://bazi" })
            ) { BaziDebugScreen(activity) }
            composable(
                route = Routes.DesignDemo,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://design" })
            ) { DesignDemoScreen(activity) }
            composable(
                route = Routes.MixAiDemo,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://mixai" })
            ) { MixAiDemoScreen(activity) }
            composable(
                route = Routes.ZiweiDemo,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://ziwei" })
            ) { ZiweiDemoScreen(activity) }
            composable(
                route = Routes.IchingDemo,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://iching" })
            ) { IchingDemoScreen(activity) }
            composable(
                route = Routes.AstroDemo,
                deepLinks = listOf(navDeepLink { uriPattern = "aidm://astro" })
            ) {
                // Demo: 固定時間/地點
                val instant = java.time.Instant.ofEpochSecond(1_700_000_000)
                val lat = 25.04; val lon = 121.56
                val planets = AstroCalculator.computePlanets(instant, lat, lon)
                val houses = AstroCalculator.computeHouses(instant, lat, lon)
                val aspects = AstroCalculator.computeAspects(planets)
                NatalChartScreen(planets = planets, houses = houses, aspects = aspects)
            }
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
        Button(onClick = { nav.navigate(Routes.DesignDemo) }) { Text("Design") }
        Button(onClick = { nav.navigate(Routes.MixAiDemo) }) { Text("Mix-AI") }
    }
}
