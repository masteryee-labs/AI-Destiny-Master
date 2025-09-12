package com.aidestinymaster.app.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.aidestinymaster.app.report.ReportViewModel
import com.aidestinymaster.app.nav.Routes
import com.aidestinymaster.app.report.ReportPrefs
import com.aidestinymaster.data.db.DatabaseProvider
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.aidestinymaster.core.lunar.AlmanacEngine
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.aidestinymaster.app.work.AiReportWorker
import java.util.Locale
import java.time.Instant
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.location.LocationManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.aidestinymaster.app.settings.SettingsPrefs
import com.aidestinymaster.core.astro.AstroCalculator.HouseSystem
import com.aidestinymaster.core.astro.AstroCalculator

@Composable
fun HomeScreen(activity: ComponentActivity, nav: NavController) {
    val viewModel = remember { ViewModelProvider(activity)[ReportViewModel::class.java] }
    val lastId by viewModel.lastId.collectAsState(null)
    val current by viewModel.current.collectAsState(null)
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("demo") }
    var content by remember { mutableStateOf("Hello at " + System.currentTimeMillis()) }
    var favQuery by remember { mutableStateOf("") }
    val favs by ReportPrefs.favsFlow(activity).collectAsState(initial = emptySet())
    var favItems by remember { mutableStateOf(listOf<com.aidestinymaster.data.db.ReportEntity>()) }
    LaunchedEffect(favs, favQuery) {
        val dao = DatabaseProvider.get(activity).reportDao()
        val all = favs.mapNotNull { id -> dao.getById(id) }
        favItems = if (favQuery.isBlank()) all else all.filter { it.title.contains(favQuery) || it.summary.contains(favQuery) }
    }

    // Daily Almanac
    val today = remember { LocalDate.now() }
    var almanac by remember { mutableStateOf<com.aidestinymaster.core.lunar.AlmanacEngine.AlmanacDay?>(null) }
    LaunchedEffect(today) {
        almanac = AlmanacEngine.getAlmanac(today)
    }

    // Astro Summary (simple, offline)
    var astroSummary by remember { mutableStateOf("") }
    var astroDetails by remember { mutableStateOf(listOf<String>()) }
    // Settings for astro: language override and house system
    val astroSettings by SettingsPrefs.flow(activity).collectAsState(initial = SettingsPrefs.Settings())
    // runtime permission request for coarse location
    var askedLoc by remember { mutableStateOf(false) }
    var noPermission by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(true) }
    var retryTick by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        // no-op; next LaunchedEffect will pick up last known location
    }
    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm && !askedLoc) {
            askedLoc = true
            launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        noPermission = !hasPerm
    }
    // Apply high-lat fallback setting to calculator
    LaunchedEffect(astroSettings.highLatFallback) {
        com.aidestinymaster.core.astro.AstroCalculator.highLatFallback = when (astroSettings.highLatFallback) {
            "REGIO" -> com.aidestinymaster.core.astro.AstroCalculator.HighLatFallback.REGIO
            else -> com.aidestinymaster.core.astro.AstroCalculator.HighLatFallback.ASC
        }
    }

    // Apply diagnostics toggle
    LaunchedEffect(astroSettings.diagnostics) {
        com.aidestinymaster.core.astro.AstroCalculator.diagnostics = astroSettings.diagnostics
    }

    // Current coords state from fused provider
    var coords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    fun refreshLocation() {
        retryTick++
    }
    LaunchedEffect(today, retryTick) {
        // Try approximate location (no runtime request here); fallback to Taipei
        fun getApproxLatLon(): Pair<Double, Double> {
            return try {
                val lm = activity.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                val hasPerm = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!hasPerm) return 25.04 to 121.56
                val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                val loc = providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }.maxByOrNull { it.time }
                if (loc != null) loc.latitude to loc.longitude else 25.04 to 121.56
            } catch (_: Throwable) { 25.04 to 121.56 }
        }
        locating = true
        val (lat0, lon0) = getApproxLatLon()
        // Try fused provider for fresher location
        try {
            val fused = LocationServices.getFusedLocationProviderClient(activity)
            val token = CancellationTokenSource()
            fused.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, token.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) coords = loc.latitude to loc.longitude
                    locating = false
                }
                .addOnFailureListener { locating = false }
        } catch (_: Throwable) {}
        coords = lat0 to lon0
        locating = false
    }
    LaunchedEffect(coords, astroSettings) {
        val instant = Instant.now()
        val (lat, lon) = coords ?: (25.04 to 121.56)
        val planets = AstroCalculator.computePlanets(instant, lat, lon)
        val aspects = AstroCalculator.computeAspects(planets)
        val major = aspects.take(5).joinToString { it.type.toString() + "° " + it.p1 + "-" + it.p2 }
        val isZh = when (astroSettings.language) {
            "zh" -> true
            "en" -> false
            else -> java.util.Locale.getDefault().language.startsWith("zh")
        }
        astroSummary = if (isZh) buildString {
            appendLine("今日星象摘要：")
            appendLine("行星：" + planets.longitudes.size + " 顆；重要相位：" + aspects.size + " 個")
            if (major.isNotBlank()) appendLine("重點相位：$major")
            append("建議：保持節奏、專注重點，避免過度消耗。")
        } else buildString {
            appendLine("Today's Astro Summary:")
            appendLine("Planets: " + planets.longitudes.size + "; Major aspects: " + aspects.size)
            if (major.isNotBlank()) appendLine("Highlights: $major")
            append("Tip: keep a steady pace and focus on essentials.")
        }
        // Houses and zodiac (stub: whole-sign Aries 0°)
        val houseSystem = when (astroSettings.houses) {
            "ARIES" -> HouseSystem.WHOLE_SIGN_ARIES
            else -> HouseSystem.WHOLE_SIGN_ASC
        }
        val houses = AstroCalculator.computeHouses(instant, lat, lon, houseSystem)
        fun signName(long: Double): String {
            val idx = ((long % 360 + 360) % 360) / 30.0
            return if (isZh) when (idx.toInt()) {
                0 -> "牡羊座"; 1 -> "金牛座"; 2 -> "雙子座"; 3 -> "巨蟹座"; 4 -> "獅子座"; 5 -> "處女座"
                6 -> "天秤座"; 7 -> "天蠍座"; 8 -> "射手座"; 9 -> "摩羯座"; 10 -> "水瓶座"; else -> "雙魚座"
            } else when (idx.toInt()) {
                0 -> "Aries"; 1 -> "Taurus"; 2 -> "Gemini"; 3 -> "Cancer"; 4 -> "Leo"; 5 -> "Virgo"
                6 -> "Libra"; 7 -> "Scorpio"; 8 -> "Sagittarius"; 9 -> "Capricorn"; 10 -> "Aquarius"; else -> "Pisces"
            }
        }
        fun houseIndex(long: Double): Int {
            // whole-sign: Aries 0 as house 1, each 30°
            return (((long % 360 + 360) % 360) / 30.0).toInt() + 1
        }
        val keyBodies = listOf("Sun", "Moon", "Mercury", "Venus", "Mars")
        astroDetails = keyBodies.mapNotNull { k ->
            val deg = planets.longitudes[k] ?: return@mapNotNull null
            val sign = signName(deg)
            val house = houseIndex(deg)
            if (isZh) "$k 在$sign，第${house}宮" else "$k in $sign, House ${house}"
        }
        // Add a house cusp summary line so users知道起點（也避免 lints 未使用）
        if (houses.cusps.isNotEmpty()) {
            val cusp1 = houses.cusps.first()
            val ascSign = signName(cusp1)
            astroDetails = astroDetails + (if (isZh) "第一宮起點：$ascSign (${String.format("%.0f°", cusp1 % 30)})" else "House 1 cusp: $ascSign (${String.format("%.0f°", cusp1 % 30)})")
        }
        // Add per-house one-liners（簡述）
        fun houseTip(idx: Int): String = if (isZh) when (idx) {
            1 -> "自我與起點"; 2 -> "財務與價值"; 3 -> "溝通與學習"; 4 -> "家庭與根基";
            5 -> "創造與浪漫"; 6 -> "健康與日常"; 7 -> "關係與合作"; 8 -> "資源與轉化";
            9 -> "遠行與視野"; 10 -> "事業與名望"; 11 -> "社群與目標"; else -> "潛意識與休養"
        } else when (idx) {
            1 -> "Identity & Beginnings"; 2 -> "Finances & Values"; 3 -> "Communication & Learning"; 4 -> "Home & Roots";
            5 -> "Creativity & Romance"; 6 -> "Health & Routine"; 7 -> "Relationships & Partnerships"; 8 -> "Resources & Transformation";
            9 -> "Travel & Vision"; 10 -> "Career & Reputation"; 11 -> "Community & Goals"; else -> "Subconscious & Rest"
        }
        val tipLines = keyBodies.mapNotNull { k ->
            val deg = planets.longitudes[k] ?: return@mapNotNull null
            val h = houseIndex(deg)
            if (isZh) "$k：${houseTip(h)}" else "$k: ${houseTip(h)}"
        }
        astroDetails = astroDetails + tipLines.take(3)
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Home", style = MaterialTheme.typography.titleLarge)

        // Quick Tools
        Text("工具選單", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { nav.navigate(Routes.ChartInput.replace("{kind}", "natal")) }) { Text("快速排盤") }
            Button(onClick = { nav.navigate(Routes.MixAiDemo) }) { Text("AI 綜合分析") }
            Button(onClick = { nav.navigate(Routes.IchingDemo) }) { Text("易經占卜") }
        }

        // Daily Almanac Card
        val a = almanac
        if (a != null) {
            Spacer(Modifier.height(8.dp))
            Text("每日黃曆", style = MaterialTheme.typography.titleMedium)
            val yi = if (a.yi.isNotEmpty()) a.yi.joinToString() else "—"
            val ji = if (a.ji.isNotEmpty()) a.ji.joinToString() else "—"
            Text("日期：${a.date}")
            a.solarTerm?.let { Text("節氣：$it") }
            a.ganzhiYear?.let { Text("年干支：$it") }
            a.zodiacAnimal?.let { Text("生肖：$it") }
            Text("宜：$yi")
            Text("忌：$ji")
        }

        // Astro Summary Card
        if (astroSummary.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("今日星象摘要", style = MaterialTheme.typography.titleMedium)
            if (locating) {
                Text("正在更新座標…", style = MaterialTheme.typography.bodySmall)
            } else if (noPermission) {
                Text("未授權定位，已採用預設座標（台北）。", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { refreshLocation() }) { Text("重新嘗試") }
            } else {
                Button(onClick = { refreshLocation() }) { Text("重新嘗試") }
            }
            Card(Modifier.fillMaxWidth().wrapContentHeight().padding(4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(astroSummary, style = MaterialTheme.typography.bodyMedium)
                    if (astroDetails.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        astroDetails.take(4).forEach { line -> Text("• $line", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") })
        OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch {
                val id = viewModel.create(type, content)
                nav.navigate(Routes.Report.replace("{reportId}", id))
            } }) { Text("Create & Open Report") }
            Button(onClick = { lastId?.let { nav.navigate(Routes.Report.replace("{reportId}", it)) } }, enabled = lastId != null) { Text("Open Last") }
            Button(onClick = { nav.navigate(Routes.ReportFavs) }) { Text("My Favorites") }
            Button(onClick = {
                // 快速排程背景生成，使用預設 chartId/mode/locale
                val ids = arrayOf("homeQuick")
                val data = workDataOf(
                    AiReportWorker.KEY_CHART_IDS to ids,
                    AiReportWorker.KEY_MODE to "Quick",
                    AiReportWorker.KEY_LOCALE to Locale.TAIWAN.toLanguageTag()
                )
                val req = OneTimeWorkRequestBuilder<AiReportWorker>().setInputData(data).build()
                WorkManager.getInstance(activity).enqueue(req)
            }) { Text("背景生成") }
        }
        if (current != null) {
            Spacer(Modifier.height(8.dp))
            Text("Last: ${current!!.title}")
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Favorites", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { nav.navigate(Routes.ReportFavs) }) { Text("更多") }
        }
        OutlinedTextField(value = favQuery, onValueChange = { favQuery = it }, label = { Text("Search favorites") })
        // Favorites Quick Actions Grid (2 columns)
        if (favItems.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            val rows = favItems.take(6).chunked(2)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { r ->
                        Card(Modifier.weight(1f).wrapContentHeight().padding(2.dp)) {
                            Column(Modifier.padding(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(r.title, style = MaterialTheme.typography.titleSmall)
                                    // Favorite toggle with Material Icon
                                    var toggling by remember { mutableStateOf(false) }
                                    val isFav = favs.contains(r.id)
                                    Row {
                                        IconButton(onClick = { scope.launch { toggling = true; ReportPrefs.toggleFav(activity, r.id); toggling = false } }, enabled = !toggling) {
                                            Icon(imageVector = if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = if (isFav) "移除收藏" else "加入收藏")
                                        }
                                        var menuOpen by remember { mutableStateOf(false) }
                                        val clipboard = LocalClipboardManager.current
                                        IconButton(onClick = { menuOpen = true }) { Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "更多") }
                                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                            DropdownMenuItem(text = { Text("分享") }, onClick = {
                                                menuOpen = false
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, r.title)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, r.summary)
                                                }
                                                activity.startActivity(android.content.Intent.createChooser(intent, "分享報告"))
                                            })
                                            DropdownMenuItem(text = { Text("分享連結") }, onClick = {
                                                menuOpen = false
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, r.title)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "aidm://report/${r.id}")
                                                }
                                                activity.startActivity(android.content.Intent.createChooser(intent, "分享連結"))
                                            })
                                            DropdownMenuItem(text = { Text("複製摘要") }, onClick = {
                                                menuOpen = false
                                                clipboard.setText(AnnotatedString(r.summary))
                                            })
                                            DropdownMenuItem(text = { Text("刪除報告") }, onClick = {
                                                menuOpen = false
                                                scope.launch {
                                                    val repo = com.aidestinymaster.data.repository.ReportRepository.from(activity)
                                                    repo.delete(r.id)
                                                    if (favs.contains(r.id)) ReportPrefs.toggleFav(activity, r.id)
                                                }
                                            })
                                            DropdownMenuItem(text = { Text(if (isFav) "移除收藏" else "加入收藏") }, onClick = {
                                                menuOpen = false
                                                scope.launch { toggling = true; ReportPrefs.toggleFav(activity, r.id); toggling = false }
                                            })
                                        }
                                        IconButton(onClick = { nav.navigate(Routes.Report.replace("{reportId}", r.id)) }) {
                                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "開啟報告")
                                        }
                                    }
                                }
                                Text(r.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { nav.navigate(Routes.Report.replace("{reportId}", r.id)) }) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "開啟報告")
                                    }
                                    IconButton(onClick = {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, r.title)
                                            putExtra(android.content.Intent.EXTRA_TEXT, r.summary)
                                        }
                                        activity.startActivity(android.content.Intent.createChooser(intent, "分享報告"))
                                    }) {
                                        Icon(imageVector = Icons.Filled.Share, contentDescription = "分享報告")
                                    }
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        favItems.take(5).forEach { r ->
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                    Text(r.summary, maxLines = 1)
                }
                Button(onClick = { nav.navigate(Routes.Report.replace("{reportId}", r.id)) }) { Text("Open") }
            }
        }
    }
}
