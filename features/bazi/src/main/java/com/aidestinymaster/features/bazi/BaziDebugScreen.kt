package com.aidestinymaster.features.bazi

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.aidestinymaster.core.lunar.BaziCalculator
import com.aidestinymaster.features.bazi.BaziI18n
import com.aidestinymaster.data.prefs.UserPrefsRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.net.URLDecoder
import android.util.Base64

// Top-level DataStore delegate (must not be inside a @Composable)
private val Context.dataStore by preferencesDataStore(name = "bazi_debug")

@Composable
fun BaziDebugScreen(activity: ComponentActivity) {
    val repo = remember { UserPrefsRepository.from(activity) }
    val lang by repo.langFlow.collectAsState(initial = "zh-TW")
    val now = remember { mutableStateOf(ZonedDateTime.now(ZoneId.systemDefault())) }
    val result by remember(now.value) { mutableStateOf(BaziCalculator.computeBazi(now.value)) }
    val five by remember(result) { mutableStateOf(BaziCalculator.evaluateFiveElements(result)) }
    val luck by remember(now.value) { mutableStateOf(BaziCalculator.computeLuckCycles(now.value)) }
    val ten by remember(result) { mutableStateOf(BaziCalculator.computeTenGods(result)) }

    // 權重滑桿狀態
    val stemWeight = remember { mutableStateOf(3.0f) }
    val hiddenMultiplier = remember { mutableStateOf(1.0f) }
    val monthBoost = remember { mutableStateOf(1.5f) }
    // 門派選擇（同/異極性定義）
    val schoolId = remember { mutableStateOf<String?>(null) }
    val dialogOpen = remember { mutableStateOf(false) }
    val importDialogOpen = remember { mutableStateOf(false) }
    val importText = remember { mutableStateOf("") }
    val dropdownOpen = remember { mutableStateOf(false) }
    val schoolHintExpanded = remember { mutableStateOf(false) }
    val customSameMap = remember {
        mutableStateMapOf(
            "BiJian" to false, "JieCai" to false, "ShiShen" to false, "ShangGuan" to false,
            "ZhengCai" to false, "PianCai" to false, "ZhengGuan" to false, "QiSha" to false,
            "ZhengYin" to false, "PianYin" to false
        )
    }

    // 匯入對話框（貼上 JSON 或 CSV）會在稍後（變數與函式皆宣告後）呈現

    // Ten Gods weights (per-key multipliers)
    val tenKeys = listOf("BiJian","JieCai","ShiShen","ShangGuan","ZhengCai","PianCai","ZhengGuan","QiSha","ZhengYin","PianYin")
    val tenWeights = remember {
        mutableMapOf(
            "BiJian" to 1.0f, "JieCai" to 1.0f, "ShiShen" to 1.0f, "ShangGuan" to 1.0f,
            "ZhengCai" to 1.0f, "PianCai" to 1.0f, "ZhengGuan" to 1.0f, "QiSha" to 1.0f,
            "ZhengYin" to 1.0f, "PianYin" to 1.0f
        )
    }

    // Polarity controls
    val samePolarityMult = remember { mutableStateOf(1.0f) }
    val diffPolarityMult = remember { mutableStateOf(1.0f) }
    val yinYangFactor = remember { mutableStateOf(1.0f) } // 若日主為陽，套用於同極；若日主為陰，套用於異極

    fun isYangStem(stem: String): Boolean = when (stem) {
        "甲","丙","戊","庚","壬","Jia","Bing","Wu","Geng","Ren" -> true
        else -> false
    }
    // 使用 BaziI18n 的同/異極性定義，便於切換門派

    // 視覺化模式：bar / pie / radar（提前宣告供 deep link 使用）
    val vizMode = remember { mutableStateOf("bar") }

    // 計算十神總量與百分比（套用權重 + 極性 + 陰陽校正）
    val adjustedTen = remember(ten, tenWeights.values.toList(), samePolarityMult.value, diffPolarityMult.value, yinYangFactor.value, schoolId.value, result.day.stem) {
        val yangDay = isYangStem(result.day.stem)
        ten.relationships.mapValues { (k,v) ->
            val base = v.toFloat() * (tenWeights[k] ?: 1.0f)
            val same = BaziI18n.isSamePolarityTenGod(k, schoolId.value)
            val polMult = if (same) samePolarityMult.value else diffPolarityMult.value
            val yy = if (same && yangDay) yinYangFactor.value else if (!same && !yangDay) yinYangFactor.value else 1.0f
            kotlin.math.round(base * polMult * yy).toInt()
        }
    }
    val totalTen = remember(adjustedTen) { adjustedTen.values.sum().coerceAtLeast(1) }

    fun applyProfileRaw(raw: String) {
        try {
            // Try direct JSON first
            val obj = JSONObject(raw)
            obj.optString("viz").let { v -> if (v in listOf("bar","pie","radar")) vizMode.value = v }
            obj.optString("school").let { s -> if (s.isNotBlank()) schoolId.value = s }
            if (obj.has("sameSet")) {
                val arr = obj.getJSONArray("sameSet")
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) set.add(arr.getString(i))
                BaziI18n.setCustomSameSet(set)
                tenKeys.forEach { k -> customSameMap[k] = set.contains(k) }
                schoolId.value = "custom"
            }
            obj.optJSONObject("multipliers")?.let { m ->
                m.optDouble("samePol").let { if (!it.isNaN()) samePolarityMult.value = it.toFloat() }
                m.optDouble("diffPol").let { if (!it.isNaN()) diffPolarityMult.value = it.toFloat() }
                m.optDouble("yinYang").let { if (!it.isNaN()) yinYangFactor.value = it.toFloat() }
            }
            obj.optJSONObject("fiveWeights")?.let { fw ->
                fw.optDouble("stem").let { if (!it.isNaN()) stemWeight.value = it.toFloat() }
                fw.optDouble("hidden").let { if (!it.isNaN()) hiddenMultiplier.value = it.toFloat() }
                fw.optDouble("monthBoost").let { if (!it.isNaN()) monthBoost.value = it.toFloat() }
            }
            obj.optJSONObject("tenWeights")?.let { tw ->
                tenKeys.forEach { k ->
                    if (tw.has(k)) tenWeights[k] = tw.optDouble(k, 1.0).toFloat()
                }
            }
        } catch (_: Exception) {
            // ignore bad json
        }
    }

    // Accept initial weights via deep link query: wStem, hMul, mBoost, tg_<key>
    androidx.compose.runtime.LaunchedEffect(Unit) {
        activity.intent?.data?.let { uri ->
            uri.getQueryParameter("wStem")?.toFloatOrNull()?.let { stemWeight.value = it }
            uri.getQueryParameter("hMul")?.toFloatOrNull()?.let { hiddenMultiplier.value = it }
            uri.getQueryParameter("mBoost")?.toFloatOrNull()?.let { monthBoost.value = it }
            // read per-ten-god and polarity/yin-yang factors
            samePolarityMult.value = uri.getQueryParameter("samePol")?.toFloatOrNull() ?: samePolarityMult.value
            diffPolarityMult.value = uri.getQueryParameter("diffPol")?.toFloatOrNull() ?: diffPolarityMult.value
            yinYangFactor.value = uri.getQueryParameter("yinYang")?.toFloatOrNull() ?: yinYangFactor.value
            // school selector & visualization mode
            uri.getQueryParameter("school")?.let { schoolId.value = it }
            uri.getQueryParameter("viz")?.let { v ->
                when (v) {
                    "bar","pie","radar" -> vizMode.value = v
                }
            }
            tenKeys.forEach { k ->
                uri.getQueryParameter("tg_"+k)?.toFloatOrNull()?.let { tenWeights[k] = it }
            }
            // profile JSON: url-encoded (optionally base64) single param to reconstruct full profile
            uri.getQueryParameter("profile")?.let { enc ->
                try {
                    val raw = URLDecoder.decode(enc, "UTF-8")
                    // try raw json
                    try { applyProfileRaw(raw) } catch (_: Exception) {}
                    // try base64 -> json
                    try {
                        val decoded = String(Base64.decode(raw, Base64.DEFAULT))
                        applyProfileRaw(decoded)
                    } catch (_: Exception) {}
                } catch (_: Exception) { /* ignore malformed profile */ }
            }
        }
    }
    val fiveWeighted by remember(result, stemWeight.value, hiddenMultiplier.value, monthBoost.value) {
        mutableStateOf(
            BaziCalculator.evaluateFiveElementsWeighted(
                result,
                stemWeight = stemWeight.value.toDouble(),
                hiddenMultiplier = hiddenMultiplier.value.toDouble(),
                monthHiddenBoost = monthBoost.value.toDouble()
            )
        )
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // 視覺化模式宣告已提前

    // DataStore 設定
    val KEY_W_STEM = floatPreferencesKey("wStem")
    val KEY_H_MUL = floatPreferencesKey("hMul")
    val KEY_M_BOOST = floatPreferencesKey("mBoost")
    val KEY_SAME = floatPreferencesKey("samePol")
    val KEY_DIFF = floatPreferencesKey("diffPol")
    val KEY_YY = floatPreferencesKey("yinYang")
    val KEY_SCHOOL = stringPreferencesKey("school")
    val KEY_VIZ = stringPreferencesKey("viz")
    val KEY_CUSTOM_SAME = stringPreferencesKey("customSameSet")

    suspend fun savePrefsDS(ctx: Context) {
        ctx.dataStore.edit { p ->
            p[KEY_W_STEM] = stemWeight.value
            p[KEY_H_MUL] = hiddenMultiplier.value
            p[KEY_M_BOOST] = monthBoost.value
            p[KEY_SAME] = samePolarityMult.value
            p[KEY_DIFF] = diffPolarityMult.value
            p[KEY_YY] = yinYangFactor.value
            p[KEY_SCHOOL] = (schoolId.value ?: BaziI18n.getPolaritySchoolOrDefault(null).id)
            p[KEY_VIZ] = vizMode.value
            if (schoolId.value == "custom") {
                val selected = customSameMap.filter { it.value == true }.keys.joinToString(",")
                p[KEY_CUSTOM_SAME] = selected
            }
            tenKeys.forEach { k -> p[floatPreferencesKey("tg_"+k)] = tenWeights[k] ?: 1.0f }
        }
    }
    suspend fun loadPrefsDS(ctx: Context) {
        val p = try { ctx.dataStore.data.first() } catch (e: Exception) { emptyPreferences() }
        stemWeight.value = p[KEY_W_STEM] ?: 3.0f
        hiddenMultiplier.value = p[KEY_H_MUL] ?: 1.0f
        monthBoost.value = p[KEY_M_BOOST] ?: 1.5f
        samePolarityMult.value = p[KEY_SAME] ?: 1.0f
        diffPolarityMult.value = p[KEY_DIFF] ?: 1.0f
        yinYangFactor.value = p[KEY_YY] ?: 1.0f
        schoolId.value = p[KEY_SCHOOL]
        // restore custom same set if exists
        p[KEY_CUSTOM_SAME]?.let { csv ->
            val set = csv.split(',').filter { it.isNotBlank() }.toSet()
            BaziI18n.setCustomSameSet(set)
            customSameMap.keys.forEach { k -> customSameMap[k] = set.contains(k) }
        }
        p[KEY_VIZ]?.let { v -> if (v in listOf("bar","pie","radar")) vizMode.value = v }
        tenKeys.forEach { k -> tenWeights[k] = p[floatPreferencesKey("tg_"+k)] ?: 1.0f }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("八字調試", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { now.value = ZonedDateTime.now(ZoneId.systemDefault()) }) { Text("現在") }
            Button(onClick = { now.value = ZonedDateTime.of(2024, 2, 4, 5, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("立春 2024") }
            Button(onClick = { now.value = ZonedDateTime.of(2023, 12, 22, 0, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("冬至 2023") }
        }
        // 一鍵基準與樣本切換
        fun applyBaselineDefaultPie() {
            schoolId.value = "default"
            vizMode.value = "pie"
            scope.launch { savePrefsDS(activity) }
        }
        fun applySampleAltBRadar() {
            schoolId.value = "altB"
            vizMode.value = "radar"
            // 固定預設 multipliers：samePol=1.2, diffPol=0.9, yinYang=1.1
            samePolarityMult.value = 1.2f
            diffPolarityMult.value = 0.9f
            yinYangFactor.value = 1.1f
            scope.launch { savePrefsDS(activity) }
        }
        fun applySampleCustomBar() {
            // 自訂同極清單：比肩、劫財、正印、偏印
            val set = setOf("BiJian","JieCai","ZhengYin","PianYin")
            BaziI18n.setCustomSameSet(set)
            tenKeys.forEach { k -> customSameMap[k] = set.contains(k) }
            schoolId.value = "custom"
            vizMode.value = "bar"
            scope.launch { savePrefsDS(activity) }
        }
        fun applySampleAltDPie() {
            // 校別 altD：印星+官殺同極；強化印/官殺的十神權重
            schoolId.value = "altD"
            vizMode.value = "pie"
            // 提升印/官殺權重，其餘維持 1.0
            tenWeights["ZhengYin"] = 1.3f
            tenWeights["PianYin"] = 1.3f
            tenWeights["ZhengGuan"] = 1.3f
            tenWeights["QiSha"] = 1.3f
            scope.launch { savePrefsDS(activity) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { applyBaselineDefaultPie() }) { Text("基準：default + pie") }
            Button(onClick = { applySampleAltBRadar() }) { Text("樣本A：altB + radar") }
            Button(onClick = { applySampleCustomBar() }) { Text("樣本B：custom + bar") }
            Button(onClick = { scope.launch { loadPrefsDS(activity) } }) { Text("恢復上次保存") }
            Button(onClick = { applySampleAltDPie() }) { Text("樣本C：altD + pie") }
        }
        // 針對三組（含第四組）提供「複製連結」方便分享
        val clipboardQL = LocalClipboardManager.current
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val json = '{' +
                    "\"version\":" + BaziI18n.POLARITY_SCHEMA_VERSION + ',' +
                    "\"school\":\"default\",\"viz\":\"pie\"" +
                    '}'
                val b64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
                val enc = java.net.URLEncoder.encode(b64, "UTF-8")
                clipboardQL.setText(AnnotatedString("aidm://bazi?profile=" + enc))
            }) { Text("複製基準連結") }
            Button(onClick = {
                val json = '{' +
                    "\"version\":" + BaziI18n.POLARITY_SCHEMA_VERSION + ',' +
                    "\"school\":\"altB\",\"viz\":\"radar\",\"multipliers\":{\"samePol\":1.2,\"diffPol\":0.9,\"yinYang\":1.1}" +
                    '}'
                val b64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
                val enc = java.net.URLEncoder.encode(b64, "UTF-8")
                clipboardQL.setText(AnnotatedString("aidm://bazi?profile=" + enc))
            }) { Text("複製樣本A連結") }
            Button(onClick = {
                val sameSet = listOf("BiJian","JieCai","ZhengYin","PianYin").joinToString(",") { "\"$it\"" }
                val json = '{' +
                    "\"version\":" + BaziI18n.POLARITY_SCHEMA_VERSION + ',' +
                    "\"school\":\"custom\",\"viz\":\"bar\",\"sameSet\":[" + sameSet + "]" +
                    '}'
                val b64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
                val enc = java.net.URLEncoder.encode(b64, "UTF-8")
                clipboardQL.setText(AnnotatedString("aidm://bazi?profile=" + enc))
            }) { Text("複製樣本B連結") }
            Button(onClick = {
                val tw = listOf("ZhengYin","PianYin","ZhengGuan","QiSha").joinToString(",") { "\"$it\":1.3" }
                val json = '{' +
                    "\"version\":" + BaziI18n.POLARITY_SCHEMA_VERSION + ',' +
                    "\"school\":\"altD\",\"viz\":\"pie\",\"tenWeights\":{" + tw + "}" +
                    '}'
                val b64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
                val enc = java.net.URLEncoder.encode(b64, "UTF-8")
                clipboardQL.setText(AnnotatedString("aidm://bazi?profile=" + enc))
            }) { Text("複製樣本C連結") }
        }
        // 更多節氣樣本：驚蟄、清明、夏至、秋分
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { now.value = ZonedDateTime.of(2024, 3, 5, 6, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("驚蟄 2024") }
            Button(onClick = { now.value = ZonedDateTime.of(2024, 4, 4, 6, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("清明 2024") }
            Button(onClick = { now.value = ZonedDateTime.of(2024, 6, 21, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("夏至 2024") }
            Button(onClick = { now.value = ZonedDateTime.of(2024, 9, 22, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("秋分 2024") }
        }
        // 清明/秋分 前一天 / 後一天
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { now.value = ZonedDateTime.of(2024, 4, 3, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("清明前一天") }
            Button(onClick = { now.value = ZonedDateTime.of(2024, 4, 5, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("清明後一天") }
            Button(onClick = { now.value = ZonedDateTime.of(2024, 9, 21, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("秋分前一天") }
            Button(onClick = { now.value = ZonedDateTime.of(2024, 9, 23, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")) }) { Text("秋分後一天") }
        }
        // 語言切換 zh-TW / en
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { repo.setLang("zh-TW") } }) { Text("繁中") }
            Button(onClick = { scope.launch { repo.setLang("en") } }) { Text("English") }
        }
        // 門派說明/可視化提示 + 彈出對話框
        val currentSchool = BaziI18n.getPolaritySchoolOrDefault(schoolId.value)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 視覺模式小色條預覽
            val vizColor = when (vizMode.value) {
                "bar" -> Color(0xFF43A047) // green
                "pie" -> Color(0xFFFFA000) // amber
                else -> Color(0xFF1E88E5)   // blue
            }
            Box(Modifier.width(6.dp).height(24.dp).background(vizColor))
            // 下拉選單選擇門派（精簡 UI）
            Button(onClick = { dropdownOpen.value = true }) { Text("校別：" + currentSchool.nameZh) }
            DropdownMenu(expanded = dropdownOpen.value, onDismissRequest = { dropdownOpen.value = false }) {
                BaziI18n.listPolaritySchools().forEach { s ->
                    val cnt = s.sameSet.size
                    val label = if (s.id == currentSchool.id) "(*) ${s.nameZh} ($cnt)" else "${s.nameZh} ($cnt)"
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { schoolId.value = s.id; dropdownOpen.value = false }
                    )
                }
                val customCnt = (if (schoolId.value == "custom") customSameMap.filter { it.value == true }.size else BaziI18n.getCustomSameSet().size)
                DropdownMenuItem(text = { Text(if (schoolId.value == "custom") "(*) 自訂 ($customCnt)" else "自訂 ($customCnt)") }, onClick = {
                    schoolId.value = "custom"; dropdownOpen.value = false
                })
                // 視覺化快速切換（含小色條）
                DropdownMenuItem(text = { Row { Box(Modifier.width(8.dp).height(16.dp).background(Color(0xFF43A047))) ; Spacer(Modifier.width(6.dp)) ; Text("視覺：條圖") } }, onClick = { vizMode.value = "bar"; dropdownOpen.value = false })
                DropdownMenuItem(text = { Row { Box(Modifier.width(8.dp).height(16.dp).background(Color(0xFFFFA000))) ; Spacer(Modifier.width(6.dp)) ; Text("視覺：圓餅") } }, onClick = { vizMode.value = "pie"; dropdownOpen.value = false })
                DropdownMenuItem(text = { Row { Box(Modifier.width(8.dp).height(16.dp).background(Color(0xFF1E88E5))) ; Spacer(Modifier.width(6.dp)) ; Text("視覺：雷達") } }, onClick = { vizMode.value = "radar"; dropdownOpen.value = false })
                DropdownMenuItem(text = { Text("門派說明/選擇…") }, onClick = { dropdownOpen.value = false; dialogOpen.value = true })
            }
            // 說明摺疊卡開關
            Button(onClick = { schoolHintExpanded.value = !schoolHintExpanded.value }) { Text(if (schoolHintExpanded.value) "收合說明" else "展開說明") }
            // 匯出/匯入
            val clipboard = LocalClipboardManager.current
            Button(onClick = {
                // 匯出 JSON（含 schema version / 備註 / tenWeights / multipliers / five-elements weights / viz）
                val sameSet = if (schoolId.value == "custom") customSameMap.filter { it.value == true }.keys else currentSchool.sameSet
                val note = "exported=" + now.value.toString()
                val tenWeightsJson = tenKeys.joinToString(",") { k -> "\"$k\":" + (tenWeights[k] ?: 1.0f) }
                val json = buildString {
                    append('{')
                    append("\"version\":").append(BaziI18n.POLARITY_SCHEMA_VERSION).append(',')
                    append("\"school\":\"").append(schoolId.value ?: currentSchool.id).append("\",")
                    append("\"sameSet\":[").append(sameSet.joinToString(",") { "\"$it\"" }).append("],")
                    append("\"note\":\"").append(note.replace("\"","'")).append("\",")
                    append("\"viz\":\"").append(vizMode.value).append("\",")
                    append("\"multipliers\":{")
                    append("\"samePol\":").append(samePolarityMult.value).append(',')
                    append("\"diffPol\":").append(diffPolarityMult.value).append(',')
                    append("\"yinYang\":").append(yinYangFactor.value).append('}')
                    append(',')
                    append("\"fiveWeights\":{")
                    append("\"stem\":").append(stemWeight.value).append(',')
                    append("\"hidden\":").append(hiddenMultiplier.value).append(',')
                    append("\"monthBoost\":").append(monthBoost.value).append('}')
                    append(',')
                    append("\"tenWeights\":{").append(tenWeightsJson).append('}')
                    append('}')
                }
                clipboard.setText(AnnotatedString(json))
            }) { Text("匯出JSON") }
            // 複製深連結（Base64 + URL encode profile）
            Button(onClick = {
                val json = clipboard.getText()?.text ?: run {
                    // 若使用者尚未匯出JSON，則即時生成當前 profile JSON
                    val s = if (schoolId.value == "custom") customSameMap.filter { it.value == true }.keys else currentSchool.sameSet
                    val tw = tenKeys.joinToString(",") { k -> "\"$k\":" + (tenWeights[k] ?: 1.0f) }
                    buildString {
                        append('{')
                        append("\"version\":").append(BaziI18n.POLARITY_SCHEMA_VERSION).append(',')
                        append("\"school\":\"").append(schoolId.value ?: currentSchool.id).append("\",")
                        append("\"sameSet\":[").append(s.joinToString(",") { "\"$it\"" }).append("],")
                        append("\"viz\":\"").append(vizMode.value).append("\",")
                        append("\"multipliers\":{")
                        append("\"samePol\":").append(samePolarityMult.value).append(',')
                        append("\"diffPol\":").append(diffPolarityMult.value).append(',')
                        append("\"yinYang\":").append(yinYangFactor.value).append('}')
                        append(',')
                        append("\"fiveWeights\":{")
                        append("\"stem\":").append(stemWeight.value).append(',')
                        append("\"hidden\":").append(hiddenMultiplier.value).append(',')
                        append("\"monthBoost\":").append(monthBoost.value).append('}')
                        append(',')
                        append("\"tenWeights\":{").append(tw).append('}')
                        append('}')
                    }
                }
                val b64 = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
                val enc = java.net.URLEncoder.encode(b64, "UTF-8")
                val link = "aidm://bazi?profile=" + enc
                clipboard.setText(AnnotatedString(link))
            }) { Text("複製連結") }
            // 從剪貼簿載入 profile 連結或JSON
            Button(onClick = {
                val txt = clipboard.getText()?.text?.trim() ?: return@Button
                // 支援直接 profile 連結或 JSON/base64 JSON
                if (txt.startsWith("aidm://bazi")) {
                    val uri = android.net.Uri.parse(txt)
                    uri.getQueryParameter("profile")?.let { enc ->
                        try {
                            val raw = URLDecoder.decode(enc, "UTF-8")
                            try { applyProfileRaw(raw) } catch (_: Exception) {}
                            try {
                                val decoded = String(Base64.decode(raw, Base64.DEFAULT))
                                applyProfileRaw(decoded)
                            } catch (_: Exception) {}
                        } catch (_: Exception) {}
                    }
                } else {
                    // 嘗試視為 JSON 或 base64 JSON
                    try { applyProfileRaw(txt) } catch (_: Exception) {}
                    try {
                        val decoded = String(Base64.decode(txt, Base64.DEFAULT))
                        applyProfileRaw(decoded)
                    } catch (_: Exception) {}
                }
            }) { Text("從剪貼簿載入") }
            Button(onClick = {
                // 匯出 CSV
                val sameSet = if (schoolId.value == "custom") customSameMap.filter { it.value == true }.keys
                              else currentSchool.sameSet
                clipboard.setText(AnnotatedString(sameSet.joinToString(",")))
            }) { Text("匯出CSV") }
            Button(onClick = { importText.value = ""; importDialogOpen.value = true }) { Text("匯入…") }
        }
        // 同極清單提示
        if (schoolHintExpanded.value) {
            val sameSet = if (schoolId.value == "custom") BaziI18n.getCustomSameSet() else currentSchool.sameSet
            val sameListLabels = sameSet.map { BaziI18n.labelTenGod(it, lang) }
            Text("同極清單：" + (if (sameListLabels.isEmpty()) "(無)" else sameListLabels.joinToString("、")))
        }
        // 保存/恢復偏好（DataStore）
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { savePrefsDS(activity) } }) { Text("保存偏好") }
            Button(onClick = { scope.launch { loadPrefsDS(activity) } }) { Text("恢復偏好") }
        }
        // 視覺化切換
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vizMode.value = "bar" }) { Text("條圖") }
            Button(onClick = { vizMode.value = "pie" }) { Text("圓餅") }
            Button(onClick = { vizMode.value = "radar" }) { Text("雷達") }
        }
        Spacer(Modifier.height(8.dp))
        Text("時間：" + now.value.toString())
        Text("生肖：" + BaziI18n.labelZodiac(result.zodiacAnimal, lang))
        Text("節氣：" + BaziI18n.labelSolarTerm(result.solarTerm, lang))
        Text(
            "年柱：" + BaziI18n.labelStem(result.year.stem, lang) + BaziI18n.labelBranch(result.year.branch, lang) +
                "  月柱：" + BaziI18n.labelStem(result.month.stem, lang) + BaziI18n.labelBranch(result.month.branch, lang)
        )
        Text(
            "日柱：" + BaziI18n.labelStem(result.day.stem, lang) + BaziI18n.labelBranch(result.day.branch, lang) +
                "  時柱：" + (result.hour?.let { BaziI18n.labelStem(it.stem, lang) + BaziI18n.labelBranch(it.branch, lang) } ?: "?")
        )
        Text("五行分佈：木${five.wood} 火${five.fire} 土${five.earth} 金${five.metal} 水${five.water}")
        // 權重滑桿
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("權重：天干=${String.format("%.1f", stemWeight.value)} 藏干x=${String.format("%.1f", hiddenMultiplier.value)} 月令x=${String.format("%.1f", monthBoost.value)}")
            Button(onClick = {
                stemWeight.value = 3.0f; hiddenMultiplier.value = 1.0f; monthBoost.value = 1.5f
                tenKeys.forEach { tenWeights[it] = 1.0f }
            }) { Text("重置") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("天干")
            Slider(value = stemWeight.value, onValueChange = { stemWeight.value = it }, valueRange = 1f..5f, steps = 3, colors = SliderDefaults.colors())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("藏干")
            Slider(value = hiddenMultiplier.value, onValueChange = { hiddenMultiplier.value = it }, valueRange = 0.5f..2.0f, steps = 3)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("月令")
            Slider(value = monthBoost.value, onValueChange = { monthBoost.value = it }, valueRange = 1.0f..2.5f, steps = 3)
        }
        // 極性與陰陽校正
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("同極x")
            Slider(value = samePolarityMult.value, onValueChange = { samePolarityMult.value = it }, valueRange = 0.5f..2.5f, steps = 4)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("異極x")
            Slider(value = diffPolarityMult.value, onValueChange = { diffPolarityMult.value = it }, valueRange = 0.5f..2.5f, steps = 4)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("陰陽校正x")
            Slider(value = yinYangFactor.value, onValueChange = { yinYangFactor.value = it }, valueRange = 0.5f..2.0f, steps = 3)
        }
        // 五行比例視覺化（加權後）：Canvas（條圖 / 圓餅 / 雷達）
        val total5 = (fiveWeighted.wood + fiveWeighted.fire + fiveWeighted.earth + fiveWeighted.metal + fiveWeighted.water).coerceAtLeast(1)
        Spacer(Modifier.height(8.dp))
        Text("五行比例（加權）：")
        val parts = listOf(
            Triple("木", fiveWeighted.wood, Color(0xFF43A047)),
            Triple("火", fiveWeighted.fire, Color(0xFFE53935)),
            Triple("土", fiveWeighted.earth, Color(0xFF8D6E63)),
            Triple("金", fiveWeighted.metal, Color(0xFF757575)),
            Triple("水", fiveWeighted.water, Color(0xFF1E88E5))
        )
        when (vizMode.value) {
            "bar" -> {
                Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                    val rowH = size.height / parts.size
                    parts.forEachIndexed { idx, (_, value, color) ->
                        val pct = value.toFloat() / total5.toFloat()
                        val barW = size.width * pct.coerceIn(0f, 1f)
                        drawRect(color = Color(0x22000000), topLeft = Offset(0f, idx * rowH + rowH*0.15f), size = Size(size.width, rowH*0.7f))
                        drawRect(color = color, topLeft = Offset(0f, idx * rowH + rowH*0.15f), size = Size(barW, rowH*0.7f))
                    }
                }
            }
            "pie" -> {
                Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                    val radius = kotlin.math.min(size.width, size.height) * 0.35f
                    val center = Offset(size.width/2f, size.height/2f)
                    var startAngle = -90f
                    parts.forEach { (_, value, color) ->
                        val sweep = 360f * (value.toFloat() / total5.toFloat())
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius*2, radius*2)
                        )
                        startAngle += sweep
                    }
                }
            }
            "radar" -> {
                Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                    val cx = size.width/2f
                    val cy = size.height/2f
                    val maxR = kotlin.math.min(cx, cy) * 0.8f
                    val vals = parts.map { it.second.toFloat() / total5.toFloat() }
                    // 畫軸線與網格
                    val count = parts.size
                    for (ring in 1..3) {
                        val r = maxR * (ring / 3f)
                        // 簡化成五邊形近似
                        val pts = (0 until count).map { i ->
                            val angle = -90.0 + (360.0 * i / count)
                            val rad = Math.toRadians(angle)
                            Offset(
                                (cx + r * kotlin.math.cos(rad).toFloat()),
                                (cy + r * kotlin.math.sin(rad).toFloat())
                            )
                        }
                        for (i in 0 until count) {
                            val a = pts[i]
                            val b = pts[(i+1)%count]
                            drawLine(Color(0x33000000), a, b, strokeWidth = 2f)
                        }
                    }
                    // 值多邊形
                    val poly = (0 until parts.size).map { i ->
                        val angle = -90.0 + (360.0 * i / parts.size)
                        val rad = Math.toRadians(angle)
                        val r = maxR * vals[i].coerceIn(0f,1f)
                        Offset(cx + r * kotlin.math.cos(rad).toFloat(), cy + r * kotlin.math.sin(rad).toFloat())
                    }
                    for (i in 0 until poly.size) {
                        val a = poly[i]
                        val b = poly[(i+1)%poly.size]
                        drawLine(Color(0xFF1E88E5), a, b, strokeWidth = 4f)
                    }
                }
            }
        }
        // 圖例與數值標註
        Spacer(Modifier.height(8.dp))
        parts.forEach { (label, value, color) ->
            val pct = value * 100.0 / total5
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(18.dp).height(12.dp).background(color))
                Text(label + "：" + value + String.format(" (%.1f%%)", pct))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("十神統計（含百分比）：")
        adjustedTen.forEach { (k, v) ->
            val pct = (v * 100.0 / totalTen)
            val tag = if (BaziI18n.isSamePolarityTenGod(k, schoolId.value)) "[同]" else "[異]"
            Text("- $tag " + BaziI18n.labelTenGod(k, lang) + "：" + v + String.format("  (%.1f%%)", pct))
        }
        // 十神個別滑桿
        Spacer(Modifier.height(8.dp))
        Text("十神權重：")
        tenKeys.forEach { k ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(BaziI18n.labelTenGod(k, lang))
                Slider(value = tenWeights[k] ?: 1.0f, onValueChange = { tenWeights[k] = it }, valueRange = 0.5f..2.5f, steps = 4)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("大運（示意）：")
        luck.take(3).forEach { c ->
            Text("${c.name}：${c.start.toLocalDate()} ~ ${c.end.toLocalDate()}")
        }
    }

    // 門派選擇/說明對話框
    if (dialogOpen.value) {
        AlertDialog(
            onDismissRequest = { dialogOpen.value = false },
            confirmButton = {
                TextButton(onClick = {
                    // 若當前為自訂，套用自訂同極集合
                    if (schoolId.value == "custom") {
                        val set = customSameMap.filterValues { it }.keys.toSet()
                        BaziI18n.setCustomSameSet(set)
                    }
                    scope.launch { savePrefsDS(activity) }
                    dialogOpen.value = false
                }) { Text("完成") }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen.value = false }) { Text("關閉") }
            },
            title = { Text("門派說明與選擇") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("內建門派：")
                    BaziI18n.listPolaritySchools().forEach { s ->
                        val labels = s.sameSet.map { BaziI18n.labelTenGod(it, lang) }.joinToString("、")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { schoolId.value = s.id }) { Text(if (schoolId.value == s.id) "(*) ${s.nameZh}" else s.nameZh) }
                        }
                        Text("同極清單：$labels")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("自訂門派：")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { schoolId.value = "custom" }) { Text(if (schoolId.value == "custom") "(*) 自訂" else "自訂") }
                        Text("（勾選作為同極的十神）")
                    }
                    // 初始化自訂勾選狀態（僅首次dialog開啟或切到custom時同步）
                    if (schoolId.value == "custom") {
                        val current = BaziI18n.getCustomSameSet()
                        tenKeys.forEach { k -> if (!customSameMap.containsKey(k)) customSameMap[k] = false else Unit }
                        tenKeys.forEach { k -> customSameMap[k] = (customSameMap[k] == true) || current.contains(k) }
                    }
                    tenKeys.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { k ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Checkbox(checked = customSameMap[k] == true, onCheckedChange = { v -> customSameMap[k] = v })
                                    Text(BaziI18n.labelTenGod(k, lang))
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
