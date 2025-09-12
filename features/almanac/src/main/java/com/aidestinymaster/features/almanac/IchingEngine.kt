package com.aidestinymaster.features.almanac

import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.ZonedDateTime
import android.content.Context
import org.json.JSONObject

// 自製、簡化的六爻卦象資料結構（中性、自撰）。
data class Hexagram(val number: Int, val lines: List<Boolean>) // true=陽，false=陰

data class IchingInterpretation(
    val title: String,
    val summary: String,
    val tips: List<String>,
    val trend: String? = null,
    val notice: String? = null
)

object IchingEngine {
    // 以時間戳模 64 產生 1..64 卦序，僅供 UI 與流程驗證
    fun castHexagramByTime(now: ZonedDateTime): Hexagram {
        val n = ((now.toEpochSecond() % 64 + 64) % 64).toInt() + 1
        // 以秒數位移生成 6 爻陰陽
        val seed = now.toEpochSecond()
        val lines = (0 until 6).map { i -> ((seed shr i) and 1L) == 1L }
        return Hexagram(n, lines)
    }

    // 讀取外部 64 卦標題與建議（assets/iching_64.json），失敗則走內建中性規則
    fun interpretHexagram(hex: Hexagram, context: Context? = null): IchingInterpretation {
        context?.let { ctx ->
            val entry = loadIchingEntry(ctx, hex.number)
            if (entry != null) {
                val (title, tips, trend, notice) = entry
                val summary = buildString {
                    if (!trend.isNullOrBlank()) append("趨勢：").append(trend).append(' ')
                    if (!notice.isNullOrBlank()) append("注意：").append(notice)
                    if (isBlank() && tips.isNotEmpty()) append("一般建議：").append(tips.joinToString("；"))
                }.ifBlank { "一般建議：" + tips.joinToString("；") }
                return IchingInterpretation(title = title, summary = summary, tips = tips, trend = trend, notice = notice)
            }
        }
        // 公版/自撰中性 fallback：依陽爻數量與上下卦趨勢
        val yangCount = hex.lines.count { it }
        val title = "卦象 #${hex.number}（陽爻：${yangCount}/6）"
        val trend = when {
            hex.lines.take(3).count { it } > hex.lines.drop(3).count { it } -> "上動下靜"
            hex.lines.take(3).count { it } < hex.lines.drop(3).count { it } -> "下動上靜"
            else -> "動靜均衡"
        }
        val summary = "整體趨勢：$trend。宜順勢而為、審時度勢，避免急進。"
        val tips = listOf(
            "保持彈性：在關鍵節點前預留備案",
            "聚焦可控：先處理可掌握的小步驟",
            "同步協調：與利害關係人保持清晰溝通"
        )
        return IchingInterpretation(title, summary, tips)
    }

    private fun loadIchingEntry(context: Context, id: Int): Quad<String, List<String>, String?, String?>? {
        return runCatching {
            val json = context.assets.open("iching_64.json").use { String(it.readBytes(), Charsets.UTF_8) }
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("items")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.getInt("id") == id) {
                    val title = o.optString("title", "卦$id")
                    val tipsArr = o.optJSONArray("advice")
                    val tips = mutableListOf<String>()
                    if (tipsArr != null) {
                        for (j in 0 until tipsArr.length()) tips += tipsArr.getString(j)
                    }
                    val trend = if (o.has("trend")) o.getString("trend").takeIf { it.isNotBlank() } else null
                    val notice = if (o.has("notice")) o.getString("notice").takeIf { it.isNotBlank() } else null
                    return Quad(title, tips, trend, notice)
                }
            }
            null
        }.getOrNull()
    }
}

@Composable
fun HexagramCard(hex: Hexagram, interpretation: IchingInterpretation, modifier: Modifier = Modifier) {
    Card(modifier.padding(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(interpretation.title, style = MaterialTheme.typography.titleMedium)

            // Collapsible details (trend/notice)
            var detailsExpanded = remember { mutableStateOf(true) }
            Row(Modifier.padding(top = 8.dp).clickable { detailsExpanded.value = !detailsExpanded.value }) {
                Text(
                    if (detailsExpanded.value) "▾ 詳細" else "▸ 詳細",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (detailsExpanded.value) {
                if (!interpretation.trend.isNullOrBlank()) {
                    Text(
                        "趨勢：${interpretation.trend}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (!interpretation.notice.isNullOrBlank()) {
                    Text(
                        "注意：${interpretation.notice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Text(interpretation.summary, style = MaterialTheme.typography.bodyMedium)

            var adviceExpanded = remember { mutableStateOf(true) }
            Row(Modifier.padding(top = 8.dp).clickable { adviceExpanded.value = !adviceExpanded.value }) {
                Text(
                    if (adviceExpanded.value) "▾ 建議" else "▸ 建議",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (adviceExpanded.value && interpretation.tips.isNotEmpty()) {
                interpretation.tips.forEach { t ->
                    Text(
                        "• $t",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// 輔助資料結構（避免額外依賴）：四元組
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
