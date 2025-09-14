package com.aidestinymaster.app.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.aidestinymaster.app.report.ReportViewModel
import com.aidestinymaster.app.nav.Routes
import com.aidestinymaster.app.report.ReportPrefs
import com.aidestinymaster.app.settings.SettingsPrefs
import com.aidestinymaster.core.astro.AstroCalculator.HouseSystem
import com.aidestinymaster.core.astro.AstroCalculator
import com.aidestinymaster.app.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.platform.LocalClipboardManager
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.aidestinymaster.data.db.DatabaseProvider
import java.time.LocalDate
import java.time.Instant
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import com.aidestinymaster.app.ui.StarfieldBackground
import com.aidestinymaster.app.ui.pressScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.aidestinymaster.app.work.AiReportWorker
import java.util.Locale
import com.aidestinymaster.core.lunar.AlmanacEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(activity: ComponentActivity, nav: NavController) {
    // 動效開關（來自設定偏好）
    val astroSettings by SettingsPrefs.flow(activity).collectAsState(initial = SettingsPrefs.Settings())
    val reduceMotion = astroSettings.reduceMotion
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
    // Settings for astro: language override and house system（已於前面宣告 astroSettings）
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

    StarfieldBackground(reduceMotion = reduceMotion) {
        // Enable vertical scroll and add bottom padding so content won't be covered by dock
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val titleAlpha by animateFloatAsState(targetValue = if (reduceMotion) 1f else 1f, animationSpec = tween(350), label = "titleAlpha")
            Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Column(Modifier.padding(0.dp)) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(id = R.string.home_title), style = MaterialTheme.typography.displaySmall, modifier = Modifier.alpha(titleAlpha))
                    }
                }
            }

            // Quick Tools
            Text(stringResource(id = R.string.home_tools), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.pressScale(enabled = !reduceMotion),
                    onClick = { nav.navigate(Routes.EntryAstro) }
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_astro), contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(id = R.string.home_quick_chart), maxLines = 1)
                }
                Button(onClick = { nav.navigate(Routes.EntryBazi) }) { Text(stringResource(id = R.string.home_entry_bazi)) }
                Button(onClick = { nav.navigate(Routes.EntryZiwei) }) { Text(stringResource(id = R.string.home_entry_ziwei)) }
                Button(onClick = { nav.navigate(Routes.EntryAstro) }) { Text(stringResource(id = R.string.home_entry_astro)) }
                Button(onClick = { nav.navigate(Routes.EntryDesign) }) { Text(stringResource(id = R.string.home_entry_design)) }
                Button(onClick = { nav.navigate(Routes.Paywall) }) { Text(stringResource(id = R.string.home_ai_mixed)) }
                Button(onClick = { /* Almanac section is below; keep on home */ scope.launch { scroll.scrollTo(0) } }) { Text(stringResource(id = R.string.home_entry_almanac)) }
                Button(onClick = { nav.navigate(Routes.EntryIching) }) { Text(stringResource(id = R.string.home_entry_iching)) }
            }

            // Daily Almanac Card
            val a = almanac
            if (a != null) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(id = R.string.home_daily_almanac), style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Column(Modifier.padding(0.dp)) {
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                        Column(Modifier.padding(12.dp)) {
                        Text(stringResource(id = R.string.label_date) + "${a.date}")
                        a.solarTerm?.let { Text(stringResource(id = R.string.label_solar_term) + "$it") }
                        a.ganzhiYear?.let { Text(stringResource(id = R.string.label_ganzhi_year) + "$it") }
                        a.zodiacAnimal?.let { Text(stringResource(id = R.string.label_zodiac) + "$it") }
                        val yi = if (a.yi.isNotEmpty()) a.yi.joinToString() else "—"
                        val ji = if (a.ji.isNotEmpty()) a.ji.joinToString() else "—"
                        Text(stringResource(id = R.string.label_yi) + "$yi")
                        Text(stringResource(id = R.string.label_ji) + "$ji")
                        }
                    }
                }
            }

            // Astro Summary Card
            if (astroSummary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(id = R.string.astro_summary_today), style = MaterialTheme.typography.titleMedium)
                if (locating) {
                    Text(stringResource(id = R.string.loc_updating), style = MaterialTheme.typography.bodySmall)
                } else if (noPermission) {
                    Text(stringResource(id = R.string.loc_no_permission), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { refreshLocation() }) { Text(stringResource(id = R.string.retry)) }
                } else {
                    Button(onClick = { refreshLocation() }) { Text(stringResource(id = R.string.retry)) }
                }
                val cardAlpha by animateFloatAsState(targetValue = if (reduceMotion) 1f else 1f, animationSpec = tween(400), label = "cardAlpha")
                Card(Modifier.fillMaxWidth().wrapContentHeight().padding(4.dp).alpha(cardAlpha)) {
                    Column(Modifier.padding(0.dp)) {
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                        Column(Modifier.padding(12.dp)) {
                        Text(astroSummary, style = MaterialTheme.typography.bodyMedium)
                        if (astroDetails.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            astroDetails.take(4).forEach { line -> Text("• $line", style = MaterialTheme.typography.bodySmall) }
                        }
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .let { base ->
                        val favDesc = stringResource(id = R.string.favorites)
                        base.semantics {
                            contentDescription = favDesc
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(id = R.string.favorites), style = MaterialTheme.typography.titleMedium)
                Button(onClick = { nav.navigate(Routes.ReportFavs) }) { Text(stringResource(id = R.string.more)) }
            }
            favItems.take(5).forEach { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .semantics {
                            contentDescription = r.title
                        },
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(r.summary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    Button(onClick = { nav.navigate(Routes.Report.replace("{reportId}", r.id)) }) { Text(stringResource(id = R.string.open)) }
                    IconButton(
                        modifier = run {
                            val desc = stringResource(id = R.string.desc_open_report)
                            Modifier.semantics { contentDescription = desc }
                        },
                        onClick = { nav.navigate(Routes.Report.replace("{reportId}", r.id)) }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(id = R.string.desc_open_report))
                    }
                    IconButton(
                        modifier = run {
                            val desc = stringResource(id = R.string.desc_share_report)
                            Modifier.semantics { contentDescription = desc }
                        },
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, r.title)
                                putExtra(android.content.Intent.EXTRA_TEXT, r.summary)
                            }
                            activity.startActivity(android.content.Intent.createChooser(intent, activity.getString(R.string.share)))
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = stringResource(id = R.string.desc_share_report))
                    }
                }
            }
        }
    }
}
