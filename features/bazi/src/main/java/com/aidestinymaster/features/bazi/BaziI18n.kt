package com.aidestinymaster.features.bazi

object BaziI18n {
    const val POLARITY_SCHEMA_VERSION: Int = 2
    private val tenGodZh = mapOf(
        "BiJian" to "比肩",
        "JieCai" to "劫財",
        "ShiShen" to "食神",
        "ShangGuan" to "傷官",
        "ZhengCai" to "正財",
        "PianCai" to "偏財",
        "ZhengGuan" to "正官",
        "QiSha" to "七殺",
        "ZhengYin" to "正印",
        "PianYin" to "偏印"
    )
    private val tenGodEn = mapOf(
        "BiJian" to "Peer",
        "JieCai" to "Rob Wealth",
        "ShiShen" to "Eating God",
        "ShangGuan" to "Hurting Officer",
        "ZhengCai" to "Direct Wealth",
        "PianCai" to "Indirect Wealth",
        "ZhengGuan" to "Direct Officer",
        "QiSha" to "Seven Killings",
        "ZhengYin" to "Direct Resource",
        "PianYin" to "Indirect Resource"
    )

    private val stemEn = mapOf(
        "甲" to "Jia", "乙" to "Yi", "丙" to "Bing", "丁" to "Ding",
        "戊" to "Wu", "己" to "Ji", "庚" to "Geng", "辛" to "Xin",
        "壬" to "Ren", "癸" to "Gui"
    )
    private val branchEn = mapOf(
        "子" to "Zi", "丑" to "Chou", "寅" to "Yin", "卯" to "Mao",
        "辰" to "Chen", "巳" to "Si", "午" to "Wu", "未" to "Wei",
        "申" to "Shen", "酉" to "You", "戌" to "Xu", "亥" to "Hai"
    )

    private val zodiacEn = mapOf(
        "鼠" to "Rat", "牛" to "Ox", "虎" to "Tiger", "兔" to "Rabbit",
        "龍" to "Dragon", "蛇" to "Snake", "馬" to "Horse", "羊" to "Goat",
        "猴" to "Monkey", "雞" to "Rooster", "狗" to "Dog", "豬" to "Pig"
    )

    private val solarTermEn = mapOf(
        "立春" to "Start of Spring",
        "冬至" to "Winter Solstice",
        "大寒" to "Great Cold",
        "小寒" to "Slight Cold"
        // 可逐步擴充 24 節氣
    )

    // 中文原生詞彙：中文界面顯示中文；英文界面顯示英文（若有）+ (中文)
    fun labelTenGod(key: String, lang: String): String {
        val zh = tenGodZh[key] ?: key
        val en = tenGodEn[key]
        return if (lang.startsWith("zh")) zh else ((en ?: key) + " (" + zh + ")")
    }

    fun labelStem(stem: String, lang: String): String {
        val en = stemEn[stem] ?: stem
        return if (lang.startsWith("zh")) stem else (en + " (" + stem + ")")
    }

    fun labelBranch(branch: String, lang: String): String {
        val en = branchEn[branch] ?: branch
        return if (lang.startsWith("zh")) branch else (en + " (" + branch + ")")
    }

    fun labelZodiac(zodiacZh: String?, lang: String): String {
        if (zodiacZh.isNullOrEmpty()) return "?"
        val en = zodiacEn[zodiacZh]
        return if (lang.startsWith("zh")) zodiacZh else ((en ?: zodiacZh) + " (" + zodiacZh + ")")
    }

    fun labelSolarTerm(nameZh: String?, lang: String): String {
        if (nameZh.isNullOrEmpty()) return if (lang.startsWith("zh")) "(無)" else "(none)"
        val en = solarTermEn[nameZh]
        return if (lang.startsWith("zh")) nameZh else ((en ?: nameZh) + " (" + nameZh + ")")
    }

    // ----- Ten Gods polarity mapping (same polarity with Day Stem) -----
    private var customSameSet: Set<String> = emptySet()
    fun setCustomSameSet(keys: Set<String>) { customSameSet = keys }
    fun getCustomSameSet(): Set<String> = customSameSet
    data class PolaritySchool(
        val id: String,
        val nameZh: String,
        val sameSet: Set<String>
    )

    // Default schools (can be extended)
    private val schools: List<PolaritySchool> = listOf(
        PolaritySchool(
            id = "default",
            nameZh = "傳統：比劫印同極",
            sameSet = setOf("BiJian", "JieCai", "ZhengYin", "PianYin")
        ),
        PolaritySchool(
            id = "altA",
            nameZh = "變體A：食傷同極",
            sameSet = setOf("BiJian", "JieCai", "ShiShen", "ShangGuan")
        ),
        PolaritySchool(
            id = "altB",
            nameZh = "變體B：財官同極",
            sameSet = setOf("ZhengCai", "PianCai", "ZhengGuan", "QiSha")
        ),
        PolaritySchool(
            id = "altC",
            nameZh = "變體C：比劫+財同極",
            sameSet = setOf("BiJian", "JieCai", "ZhengCai", "PianCai")
        ),
        PolaritySchool(
            id = "altD",
            nameZh = "變體D：印星+官殺同極",
            sameSet = setOf("ZhengYin", "PianYin", "ZhengGuan", "QiSha")
        )
    )

    fun listPolaritySchools(): List<PolaritySchool> = schools

    fun getPolaritySchoolOrDefault(id: String?): PolaritySchool =
        if (id == "custom") PolaritySchool("custom", "自訂：使用者定義", customSameSet)
        else schools.firstOrNull { it.id == id } ?: schools.first()

    fun isSamePolarityTenGod(key: String, schoolId: String?): Boolean =
        if (schoolId == "custom") customSameSet.contains(key) else getPolaritySchoolOrDefault(schoolId).sameSet.contains(key)
}
