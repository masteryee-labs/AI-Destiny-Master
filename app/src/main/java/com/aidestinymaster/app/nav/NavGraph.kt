package com.aidestinymaster.app.nav

import androidx.activity.ComponentActivity
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aidestinymaster.app.settings.SettingsScreen
import com.aidestinymaster.app.home.HomeScreen
import com.aidestinymaster.app.report.ReportScreen
import com.aidestinymaster.app.chart.ChartInputScreen
import com.aidestinymaster.app.chart.ChartResultScreen
import com.aidestinymaster.app.paywall.PaywallScreen
import com.aidestinymaster.app.onboarding.OnboardingScreen
import com.aidestinymaster.core.ai.ModelInstaller
import com.aidestinymaster.data.prefs.UserPrefsRepository
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidestinymaster.app.theme.ThemeViewModel
import com.aidestinymaster.app.theme.AppTheme
 
import kotlinx.coroutines.launch
 
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.aidestinymaster.app.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.aidestinymaster.app.ui.pressScale
import com.aidestinymaster.app.ui.FadeThrough
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search

object Routes {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Settings = "settings"
    const val ChartInput = "chartInput/{kind}"
    const val ChartResult = "chartResult/{chartId}"
    const val Report = "report/{reportId}"
    const val Paywall = "paywall"
    const val ReportFavs = "reportFavs"
    // Engine entry routes
    const val EntryBazi = "entry/bazi"
    const val EntryZiwei = "entry/ziwei"
    const val EntryAstro = "entry/astro"
    const val EntryDesign = "entry/design"
    const val EntryIching = "entry/iching"
}

@Composable
fun AppNav(activity: ComponentActivity, externalNav: NavHostController? = null, intent: Intent? = null) {
    val nav = externalNav ?: rememberNavController()
    // 移除重複的模型安裝檢查（以避免重覆呼叫）。實際安裝移至 Application/MainActivity 初始化流程。
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

        // 讀取減少動態偏好
        val ctx = LocalContext.current
        val settings by com.aidestinymaster.app.settings.SettingsPrefs.flow(ctx).collectAsState(initial = com.aidestinymaster.app.settings.SettingsPrefs.Settings())
        val reduceMotion = settings.reduceMotion
        // 監聽 Onboarding 完成狀態：用於決定是否顯示/啟用底部 Dock
        val prefsRepo = remember { UserPrefsRepository.from(ctx) }
        val onboardingDone by prefsRepo.onboardingDoneFlow.collectAsState(initial = false)

        Scaffold(
            topBar = { /* No header: full-screen look */ },
            bottomBar = {
                val backStack by nav.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route ?: start.value!!
                // 在同意流程頁（Onboarding）且尚未完成時，隱藏底部 Dock，避免使用者跳過同意步驟
                if (!(currentRoute.startsWith(Routes.Onboarding) && !onboardingDone)) {
                    BottomDock(nav, reduceMotion, currentRoute)
                }
            }
        ) { padding ->
            val backStack by nav.currentBackStackEntryAsState()
            val route = backStack?.destination?.route ?: start.value!!
            FadeThrough(target = route, reduceMotion = reduceMotion) { _ ->
                Box(Modifier.fillMaxSize().padding(padding)) {
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
                            route = Routes.Settings,
                            deepLinks = listOf(navDeepLink { uriPattern = "aidm://settings" })
                        ) { SettingsScreen(activity) }
                        // Engine entry pages with deep links
                        composable(
                            route = Routes.EntryBazi,
                            deepLinks = listOf(navDeepLink { uriPattern = "aidm://entry/bazi" })
                        ) { com.aidestinymaster.app.entry.BaziEntryScreen(activity, nav) }
                        composable(
                            route = Routes.EntryZiwei,
                            deepLinks = listOf(navDeepLink { uriPattern = "aidm://entry/ziwei" })
                        ) { com.aidestinymaster.app.entry.ZiweiEntryScreen(activity, nav) }
                        composable(
                            route = Routes.EntryAstro,
                            deepLinks = listOf(navDeepLink { uriPattern = "aidm://entry/astro" })
                        ) { com.aidestinymaster.app.entry.AstroEntryScreen(activity, nav) }
                        composable(
                            route = Routes.EntryDesign,
                            deepLinks = listOf(navDeepLink { uriPattern = "aidm://entry/design" })
                        ) { com.aidestinymaster.app.entry.DesignEntryScreen(activity, nav) }
                        composable(
                            route = Routes.EntryIching,
                            deepLinks = listOf(navDeepLink { uriPattern = "aidm://entry/iching" })
                        ) { com.aidestinymaster.app.entry.IchingEntryScreen(activity, nav) }
                        composable(
                            route = Routes.ChartInput,
                            arguments = listOf(navArgument("kind") {}),
                            deepLinks = listOf(navDeepLink { uriPattern = "aidm://chartInput/{kind}" })
                        ) { backStack -> ChartInputScreen(activity, backStack.arguments?.getString("kind") ?: "", nav) }
                        composable(
                            route = Routes.ChartResult,
                            arguments = listOf(navArgument("chartId") {}),
                            deepLinks = listOf(navDeepLink { uriPattern = "aidm://chartResult/{chartId}" })
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
                }
            }
            // Handle deep links only after NavHost has set the graph to avoid NPE
            val deepLinkHandled = remember { mutableStateOf(false) }
            val latestIntent = remember { mutableStateOf(intent) }
            LaunchedEffect(intent) {
                latestIntent.value = intent
                // allow handling a new incoming intent
                deepLinkHandled.value = false
            }
            LaunchedEffect(start.value, latestIntent.value, onboardingDone) {
                if (!deepLinkHandled.value && latestIntent.value != null && start.value != null) {
                    // Mark handled early to avoid double-entry races on recomposition
                    deepLinkHandled.value = true
                    val i = latestIntent.value
                    try {
                        val uriString = i?.dataString ?: ""
                        Log.i("AIDM", "NavGraph.handleDeepLink (post-graph) uri=" + uriString)
                        // 若尚未完成 Onboarding，忽略非 Onboarding 的 deeplink，導向同意頁
                        if (!onboardingDone && !(uriString.startsWith("aidm://onboarding") || uriString.isEmpty())) {
                            nav.navigate(Routes.Onboarding + "?from=deeplink")
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
}

@Composable
private fun PremiumHeader(nav: NavHostController, route: String, offsetPx: Float, maxCollapsePx: Float) {
    val fraction = kotlin.math.abs(offsetPx) / maxCollapsePx
    val title = when {
        route.startsWith(Routes.Home) -> "首頁"
        route.startsWith(Routes.Settings) -> "設定"
        route.startsWith(Routes.Report) -> "報告"
        route.startsWith(Routes.ChartInput) -> "排盤"
        route.startsWith(Routes.ChartResult) -> "結果"
        route.startsWith(Routes.ReportFavs) -> "我的收藏"
        route.startsWith(Routes.Onboarding) -> "導覽"
        else -> "AI命理大師"
    }
    val elev = 2.dp + (6.dp * fraction)
    Surface(shadowElevation = elev, tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
        Row(
            Modifier
                .graphicsLayer { translationY = offsetPx; alpha = 1f - 0.25f * fraction }
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            // 副標（簡短說明）
            val subtitle = when {
                route.startsWith(Routes.Home) -> "快速入口與今日黃曆"
                route.startsWith(Routes.Report) || route.startsWith(Routes.ReportFavs) -> "報告與收藏"
                route.startsWith(Routes.ChartInput) || route.startsWith(Routes.ChartResult) -> "輸入資料與結果"
                route.startsWith(Routes.Settings) -> "偏好與權益"
                else -> ""
            }
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium)
            }
            // 快捷操作：收藏、設定
            IconButton(onClick = { nav.navigate(Routes.ReportFavs) }, modifier = Modifier.semantics { contentDescription = "open_favorites" }) {
                Icon(painter = painterResource(id = R.drawable.ic_ziwei), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            // 搜尋（暫導向收藏，以利查找）
            IconButton(onClick = { nav.navigate(Routes.ReportFavs) }, modifier = Modifier.semantics { contentDescription = "open_search" }) {
                Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            // 同步（暫導向設定頁之同步入口）
            IconButton(onClick = { nav.navigate(Routes.Settings) }, modifier = Modifier.semantics { contentDescription = "open_settings" }) {
                Icon(painter = painterResource(id = R.drawable.ic_onboarding_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BottomDock(nav: NavHostController, reduceMotion: Boolean, route: String) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    // Transparent container to avoid opaque/odd blocks under the dock
    Surface(shadowElevation = 0.dp, tonalElevation = 0.dp, color = Color.Transparent) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 8.dp, top = 6.dp, end = 8.dp, bottom = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DockItem(icon = R.drawable.ic_astro, label = androidx.compose.ui.res.stringResource(id = R.string.dock_home), active = route.startsWith(Routes.Home), reduceMotion = reduceMotion, modifier = Modifier.weight(1f)) { nav.navigate(Routes.Home) }
                DockItem(icon = R.drawable.ic_bazi, label = androidx.compose.ui.res.stringResource(id = R.string.dock_charts), active = route.startsWith("chartInput"), reduceMotion = reduceMotion, modifier = Modifier.weight(1f)) { nav.navigate(Routes.ChartInput.replace("{kind}", "natal")) }
                DockItem(icon = R.drawable.ic_ziwei, label = androidx.compose.ui.res.stringResource(id = R.string.dock_reports), active = route.startsWith(Routes.Report) || route.startsWith(Routes.ReportFavs), reduceMotion = reduceMotion, modifier = Modifier.weight(1f)) { nav.navigate(Routes.ReportFavs) }
                DockItem(icon = R.drawable.ic_onboarding_check, label = androidx.compose.ui.res.stringResource(id = R.string.dock_settings), active = route.startsWith(Routes.Settings), reduceMotion = reduceMotion, modifier = Modifier.weight(1f)) { nav.navigate(Routes.Settings) }
            }
        }
    }
}

@Composable
private fun DockItem(icon: Int, label: String, active: Boolean, reduceMotion: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // 以垂直排列顯示：上方圖示，下方文字，並以圓角容器居中，避免陰影偏移
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent, shape = RoundedCornerShape(18.dp))
                .shadow(if (active) 6.dp else 0.dp, RoundedCornerShape(18.dp), clip = false)
                .pressScale(enabled = !reduceMotion)
                .clickable { onClick() }
                .semantics { contentDescription = if (active) "dock_${label.lowercase()}_active" else "dock_${label.lowercase()}" }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val iconTint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val textColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
            Icon(painter = painterResource(id = icon), contentDescription = null, tint = iconTint)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = textColor)
        }
    }
}
