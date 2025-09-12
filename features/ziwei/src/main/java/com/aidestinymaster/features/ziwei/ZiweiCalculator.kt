package com.aidestinymaster.features.ziwei

import android.content.Context
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.ChronoField

data class ZiweiStar(val name: String, val abbr: String, val nature: String)
data class ZiweiTransform(val name: String, val desc: String)
data class ZiweiPalace(val name: String, val stars: List<ZiweiStar>, val transforms: List<ZiweiTransform>)
data class ZiweiChart(val palaces: List<ZiweiPalace>)
data class ZiweiSummary(val title: String, val highlights: List<String>)

/**
 * 自製、簡化的紫微盤推導：
 * - 以出生時間的欄位（年/月/日/時/分）做哈希，將少量星曜與四化分配至 12 宮。
 * - 僅供 UI 與流程測試，不代表傳統算法。
 */
object ZiweiCalculator {
    private val palaceNames = listOf("命", "兄弟", "夫妻", "子女", "財帛", "疾厄", "遷移", "僕役", "官祿", "田宅", "福德", "父母")

    // 無 context 版本：使用內建簡表，符合任務函式簽名
    fun computeZiweiChart(birth: ZonedDateTime): ZiweiChart {
        val (stars, transforms) = fallbackTables()
        val seed = makeSeed(birth)
        val palaces = palaceNames.mapIndexed { idx, name ->
            val starsHere = pickItems(stars, seed + idx, count = 2)
            val transHere = pickItems(transforms, seed + idx * 3, count = 1)
            ZiweiPalace(name = name, stars = starsHere, transforms = transHere)
        }
        return ZiweiChart(palaces)
    }

    fun computeZiweiChart(context: Context, birth: ZonedDateTime): ZiweiChart {
        val (stars, transforms) = loadTables(context)
        val seed = makeSeed(birth)
        val palaces = palaceNames.mapIndexed { idx, name ->
            val starsHere = pickItems(stars, seed + idx, count = 2)
            val transHere = pickItems(transforms, seed + idx * 3, count = 1)
            ZiweiPalace(name = name, stars = starsHere, transforms = transHere)
        }
        return ZiweiChart(palaces)
    }

    fun summarizeZiwei(chart: ZiweiChart): ZiweiSummary {
        val topPalace = chart.palaces.maxByOrNull { it.stars.size + it.transforms.size } ?: chart.palaces.first()
        val topStars = topPalace.stars.joinToString("、") { it.name }
        val title = "紫微盤概覽：重點在${topPalace.name}宮（星曜：$topStars）"
        val highlights = buildList {
            add("整體平衡：${chart.palaces.count { it.stars.isNotEmpty() }}/12 宮有主星")
            add("可能著力領域：${topPalace.name}（四化：${topPalace.transforms.joinToString("、") { it.name }})")
            add("留意節奏：以自我節律為優先，避免過度比較（自我覺察）")
        }
        return ZiweiSummary(title, highlights)
    }

    private fun makeSeed(birth: ZonedDateTime): Int {
        val b = birth.withZoneSameInstant(ZoneId.of("UTC"))
        return b.get(ChronoField.YEAR) * 10000 + b.get(ChronoField.MONTH_OF_YEAR) * 100 + b.get(ChronoField.DAY_OF_MONTH) +
                b.get(ChronoField.HOUR_OF_DAY) + b.get(ChronoField.MINUTE_OF_HOUR)
    }

    private fun <T> pickItems(pool: List<T>, base: Int, count: Int): List<T> {
        if (pool.isEmpty()) return emptyList()
        val out = mutableListOf<T>()
        var s = (base * 1103515245 + 12345)
        repeat(count) {
            val idx = Math.floorMod(s, pool.size)
            out += pool[idx]
            s = s xor (s shl 13)
            s = s xor (s ushr 17)
            s = s xor (s shl 5)
        }
        return out
    }

    private fun loadTables(context: Context): Pair<List<ZiweiStar>, List<ZiweiTransform>> {
        val am = context.assets
        val json = am.open("ziwei_stars.json").use { String(it.readBytes(), Charsets.UTF_8) }
        val obj = JSONObject(json)
        val starsArr = obj.getJSONArray("stars")
        val transArr = obj.getJSONArray("transforms")
        val stars = buildList {
            for (i in 0 until starsArr.length()) {
                val o = starsArr.getJSONObject(i)
                add(ZiweiStar(o.getString("name"), o.getString("abbr"), o.optString("nature", "")))
            }
        }
        val transforms = buildList {
            for (i in 0 until transArr.length()) {
                val o = transArr.getJSONObject(i)
                add(ZiweiTransform(o.getString("name"), o.optString("desc", "")))
            }
        }
        return stars to transforms
    }

    private fun fallbackTables(): Pair<List<ZiweiStar>, List<ZiweiTransform>> {
        val stars = listOf(
            ZiweiStar("紫微", "ZW", "主中樞/領導傾向"),
            ZiweiStar("天機", "TJ", "主思慮/變動/策劃"),
            ZiweiStar("太陽", "TY", "主外放/光亮/熱忱"),
            ZiweiStar("武曲", "WQ", "主財務/執行/果決"),
            ZiweiStar("太陰", "TYn", "主內斂/敏感/細膩")
        )
        val transforms = listOf(
            ZiweiTransform("祿", "增益、資源流入"),
            ZiweiTransform("權", "掌控、影響力提升"),
            ZiweiTransform("科", "名望、助力"),
            ZiweiTransform("忌", "阻滯、壓力")
        )
        return stars to transforms
    }
}
