package com.aidestinymaster.core.lunar

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.time.ZonedDateTime

object BaziCalculator {
    /** Heavenly Stem + Earthly Branch */
    data class BaziPillar(
        val stem: String,
        val branch: String
    )

    /** Core BaZi result */
    data class BaziResult(
        val year: BaziPillar,
        val month: BaziPillar,
        val day: BaziPillar,
        val hour: BaziPillar?,
        val zodiacAnimal: String? = null,
        val solarTerm: String? = null,
        val metadata: Map<String, String> = emptyMap()
    )

    /** Ten Gods (Shi Shen) profile summary */
    data class TenGodsProfile(
        val selfElement: String?,
        val relationships: Map<String, Int> = emptyMap(), // e.g. {"BiJian": 2, "JieCai": 1, ...}
        val notes: String? = null
    )

    /** Five Elements balance scoring */
    data class FiveElementsScore(
        val wood: Int,
        val fire: Int,
        val earth: Int,
        val metal: Int,
        val water: Int,
        val total: Int = wood + fire + earth + metal + water
    )

    /** Luck cycle (Da Yun / Liu Nian etc.) */
    data class LuckCycle(
        val name: String,
        val start: ZonedDateTime,
        val end: ZonedDateTime,
        val elementTendency: String? = null,
        val note: String? = null
    )

    /**
     * Compute BaZi pillars for a given birth ZonedDateTime.
     * TODO: Implement using `lunar-java` once inputs/locale are finalized.
     */
    fun computeBazi(birthZonedDateTime: ZonedDateTime): BaziResult {
        val local = birthZonedDateTime.toLocalDateTime()
        val solar: Solar = Solar.fromYmdHms(
            local.year,
            local.monthValue,
            local.dayOfMonth,
            local.hour,
            local.minute,
            local.second
        )
        val lunar: Lunar = solar.getLunar()

        val yearGz = safeGanzhi(lunar.yearInGanZhi)
        val monthGz = safeGanzhi(lunar.monthInGanZhi)
        val dayGz = safeGanzhi(lunar.dayInGanZhi)
        val timeGz = safeGanzhi(lunar.timeInGanZhi)

        val year = BaziPillar(yearGz.first, yearGz.second)
        val month = BaziPillar(monthGz.first, monthGz.second)
        val day = BaziPillar(dayGz.first, dayGz.second)
        val hour = BaziPillar(timeGz.first, timeGz.second)

        val zodiac = lunar.yearShengXiao
        // 當天若正逢節氣，lunar.jieQi 會給出對應名稱；否則取最近節氣名稱
        val solarTerm = lunar.jieQi ?: nearestJieQiName(lunar, birthZonedDateTime)

        val meta = mapOf(
            "gzYear" to lunar.yearInGanZhi,
            "gzMonth" to lunar.monthInGanZhi,
            "gzDay" to lunar.dayInGanZhi,
            "gzHour" to lunar.timeInGanZhi,
            "lunarYmd" to "${lunar.year}-${lunar.month}-${lunar.day}",
            "solarYmd" to "${solar.year}-${solar.month}-${solar.day}",
            "jieQi" to (solarTerm ?: "")
        )

        return BaziResult(
            year = year,
            month = month,
            day = day,
            hour = hour,
            zodiacAnimal = zodiac,
            solarTerm = solarTerm,
            metadata = meta
        )
    }

    private fun nearestJieQiName(lunar: Lunar, zdt: ZonedDateTime): String? {
        return try {
            val table = lunar.jieQiTable // Map<String, Solar>
            var best: Pair<String, Long>? = null
            for ((name, s) in table) {
                val dt = ZonedDateTime.of(s.year, s.month, s.day, s.hour, s.minute, s.second, 0, zdt.zone)
                val diff = kotlin.math.abs(java.time.Duration.between(zdt, dt).seconds)
                if (best == null || diff < best.second) best = name to diff
            }
            best?.first
        } catch (_: Throwable) { null }
    }

    private fun safeGanzhi(gz: String?): Pair<String, String> {
        if (gz.isNullOrBlank()) return "?" to "?"
        // 常見格式為兩字（天干一字+地支一字），有些實作可能含有空白或修飾，謹慎截取前兩字
        val s = gz.trim()
        val stem = s.substring(0, minOf(1, s.length))
        val branch = if (s.length >= 2) s.substring(1, 2) else "?"
        return stem to branch
    }

    /**
     * Compute Ten Gods distribution from a BaZi result.
     * TODO: Implement real mappings from heavenly stems/earthly branches.
     */
    fun computeTenGods(bazi: BaziResult): TenGodsProfile {
        // 十神分佈（含天干與藏干）加權：天干 3.0；藏干依配比；月令藏干再乘 1.5
        val dayStem = bazi.day.stem
        val dayElement = stemToElement(dayStem)
        val countsW = mutableMapOf<String, Double>()

        fun addW(key: String?, w: Double) {
            if (key == null || w == 0.0) return
            countsW[key] = (countsW[key] ?: 0.0) + w
        }

        // 天干：年/月/時（不含自身日干）
        listOf(bazi.year.stem, bazi.month.stem, bazi.hour?.stem)
            .filterNotNull()
            .forEach { s -> addW(tenGodOf(dayStem, s), 3.0) }

        // 地支藏干：年/日/時（依配比）
        branchHiddenStems(bazi.year.branch).forEach { (s, w) -> addW(tenGodOf(dayStem, s), w) }
        branchHiddenStems(bazi.day.branch).forEach { (s, w) -> addW(tenGodOf(dayStem, s), w) }
        branchHiddenStems(bazi.hour?.branch).forEach { (s, w) -> addW(tenGodOf(dayStem, s), w) }
        // 月令藏干：加成 1.5
        branchHiddenStems(bazi.month.branch).forEach { (s, w) -> addW(tenGodOf(dayStem, s), w * 1.5) }

        // 轉為整數（四捨五入）
        val counts = countsW.mapValues { (_, v) -> kotlin.math.round(v).toInt() }.toSortedMap()
        return TenGodsProfile(
            selfElement = dayElement,
            relationships = counts,
            notes = "Weighted counts: stems=3.0, hidden by ratio, month hidden x1.5"
        )
    }

    /**
     * Evaluate Five Elements balance for the given BaZi.
     * TODO: Implement based on stems/branches elemental assignments and strength rules.
     */
    fun evaluateFiveElements(bazi: BaziResult): FiveElementsScore {
        var wood = 0.0; var fire = 0.0; var earth = 0.0; var metal = 0.0; var water = 0.0
        fun add(elem: String?, w: Double = 1.0) {
            when (elem) {
                "Wood" -> wood += w
                "Fire" -> fire += w
                "Earth" -> earth += w
                "Metal" -> metal += w
                "Water" -> water += w
            }
        }
        // 天干權重較高
        listOf(bazi.year.stem, bazi.month.stem, bazi.day.stem, bazi.hour?.stem)
            .filterNotNull().forEach { add(stemToElement(it), 3.0) }
        // 地支藏干：取加權組合；月令加成
        val hiddenYear = branchHiddenStems(bazi.year.branch)
        val hiddenMonth = branchHiddenStems(bazi.month.branch)
        val hiddenDay = branchHiddenStems(bazi.day.branch)
        val hiddenHour = branchHiddenStems(bazi.hour?.branch)
        hiddenYear.forEach { (s, w) -> add(stemToElement(s), w) }
        hiddenMonth.forEach { (s, w) -> add(stemToElement(s), w * 1.5) } // 月令加成
        hiddenDay.forEach { (s, w) -> add(stemToElement(s), w) }
        hiddenHour.forEach { (s, w) -> add(stemToElement(s), w) }
        return FiveElementsScore(
            wood = kotlin.math.round(wood).toInt(),
            fire = kotlin.math.round(fire).toInt(),
            earth = kotlin.math.round(earth).toInt(),
            metal = kotlin.math.round(metal).toInt(),
            water = kotlin.math.round(water).toInt()
        )
    }

    /** Weighted Five Elements evaluation for UI tuning. */
    fun evaluateFiveElementsWeighted(
        bazi: BaziResult,
        stemWeight: Double = 3.0,
        hiddenMultiplier: Double = 1.0,
        monthHiddenBoost: Double = 1.5
    ): FiveElementsScore {
        var wood = 0.0; var fire = 0.0; var earth = 0.0; var metal = 0.0; var water = 0.0
        fun add(elem: String?, w: Double = 1.0) {
            when (elem) {
                "Wood" -> wood += w
                "Fire" -> fire += w
                "Earth" -> earth += w
                "Metal" -> metal += w
                "Water" -> water += w
            }
        }
        listOf(bazi.year.stem, bazi.month.stem, bazi.day.stem, bazi.hour?.stem)
            .filterNotNull().forEach { add(stemToElement(it), stemWeight) }
        branchHiddenStems(bazi.year.branch).forEach { (s, w) -> add(stemToElement(s), w * hiddenMultiplier) }
        branchHiddenStems(bazi.month.branch).forEach { (s, w) -> add(stemToElement(s), w * hiddenMultiplier * monthHiddenBoost) }
        branchHiddenStems(bazi.day.branch).forEach { (s, w) -> add(stemToElement(s), w * hiddenMultiplier) }
        branchHiddenStems(bazi.hour?.branch).forEach { (s, w) -> add(stemToElement(s), w * hiddenMultiplier) }
        return FiveElementsScore(
            wood = kotlin.math.round(wood).toInt(),
            fire = kotlin.math.round(fire).toInt(),
            earth = kotlin.math.round(earth).toInt(),
            metal = kotlin.math.round(metal).toInt(),
            water = kotlin.math.round(water).toInt()
        )
    }

    /**
     * Compute luck cycles timeline from the birth data.
     * TODO: Implement Da Yun and annual cycles calculation rules.
     */
    fun computeLuckCycles(birth: ZonedDateTime): List<LuckCycle> {
        // 初版：從出生當日起算的每 10 年一運，共 8 運
        val cycles = mutableListOf<LuckCycle>()
        var s = birth
        for (i in 1..8) {
            val e = s.plusYears(10)
            cycles += LuckCycle(name = "大運$i", start = s, end = e, elementTendency = null, note = "Initial 10-year cycle")
            s = e
        }
        return cycles
    }

    private fun stemToElement(stem: String?): String? = when (stem) {
        "甲", "乙", "Jia", "Yi" -> "Wood"
        "丙", "丁", "Bing", "Ding" -> "Fire"
        "戊", "己", "Wu", "Ji" -> "Earth"
        "庚", "辛", "Geng", "Xin" -> "Metal"
        "壬", "癸", "Ren", "Gui" -> "Water"
        else -> null
    }

    private fun stemYinYang(stem: String?): String? = when (stem) {
        "甲", "丙", "戊", "庚", "壬", "Jia", "Bing", "Wu", "Geng", "Ren" -> "Yang"
        "乙", "丁", "己", "辛", "癸", "Yi", "Ding", "Ji", "Xin", "Gui" -> "Yin"
        else -> null
    }

    private fun branchToElement(branch: String?): String? = when (branch) {
        "子", "Zi" -> "Water"
        "丑", "Chou" -> "Earth"
        "寅", "Yin" -> "Wood"
        "卯", "Mao" -> "Wood"
        "辰", "Chen" -> "Earth"
        "巳", "Si" -> "Fire"
        "午", "Wu" -> "Fire"
        "未", "Wei" -> "Earth"
        "申", "Shen" -> "Metal"
        "酉", "You" -> "Metal"
        "戌", "Xu" -> "Earth"
        "亥", "Hai" -> "Water"
        else -> null
    }

    private fun branchHiddenStems(branch: String?): List<Pair<String, Double>> = when (branch) {
        // 參考常見藏干配比（簡化版本）
        "子", "Zi" -> listOf("癸" to 1.0)
        "丑", "Chou" -> listOf("己" to 0.5, "癸" to 0.3, "辛" to 0.2)
        "寅", "Yin" -> listOf("甲" to 0.6, "丙" to 0.2, "戊" to 0.2)
        "卯", "Mao" -> listOf("乙" to 1.0)
        "辰", "Chen" -> listOf("戊" to 0.6, "乙" to 0.2, "癸" to 0.2)
        "巳", "Si" -> listOf("丙" to 0.6, "戊" to 0.2, "庚" to 0.2)
        "午", "Wu" -> listOf("丁" to 0.7, "己" to 0.3)
        "未", "Wei" -> listOf("己" to 0.6, "丁" to 0.2, "乙" to 0.2)
        "申", "Shen" -> listOf("庚" to 0.7, "壬" to 0.2, "戊" to 0.1)
        "酉", "You" -> listOf("辛" to 1.0)
        "戌", "Xu" -> listOf("戊" to 0.6, "辛" to 0.2, "丁" to 0.2)
        "亥", "Hai" -> listOf("壬" to 0.8, "甲" to 0.2)
        else -> emptyList()
    }

    // 生剋關係
    private fun produces(a: String?, b: String?): Boolean =
        (a == "Wood" && b == "Fire") ||
        (a == "Fire" && b == "Earth") ||
        (a == "Earth" && b == "Metal") ||
        (a == "Metal" && b == "Water") ||
        (a == "Water" && b == "Wood")

    private fun controls(a: String?, b: String?): Boolean =
        (a == "Wood" && b == "Earth") ||
        (a == "Earth" && b == "Water") ||
        (a == "Water" && b == "Fire") ||
        (a == "Fire" && b == "Metal") ||
        (a == "Metal" && b == "Wood")

    private fun tenGodOf(selfStem: String?, otherStem: String?): String? {
        if (selfStem == null || otherStem == null) return null
        val se = stemToElement(selfStem)
        val so = stemToElement(otherStem)
        val sy = stemYinYang(selfStem)
        val oy = stemYinYang(otherStem)
        val samePolarity = (sy != null && oy != null && sy == oy)
        return when {
            se == so -> if (samePolarity) "BiJian" else "JieCai"
            produces(se, so) -> if (samePolarity) "ShiShen" else "ShangGuan"
            produces(so, se) -> if (samePolarity) "ZhengYin" else "PianYin"
            controls(se, so) -> if (samePolarity) "ZhengCai" else "PianCai"
            controls(so, se) -> if (samePolarity) "ZhengGuan" else "QiSha"
            else -> null
        }
    }
}
