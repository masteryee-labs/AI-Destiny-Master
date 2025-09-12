@file:Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")

package com.aidestinymaster.core.astro

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.abs
import kotlin.math.floor
import android.util.Log
import kotlin.math.min

/**
 * 天文計算門面：使用 Astronomy Engine 取得真實行星位置；
 * 宮位目前提供 Whole-Sign 簡化（以 0° 牡羊起算），後續可升級為 Asc 爲起點。
 */
object AstroCalculator {

    enum class HouseSystem { WHOLE_SIGN_ASC, WHOLE_SIGN_ARIES, PLACIDUS }
    enum class HighLatFallback { ASC, REGIO }

    data class PlanetPositions(val longitudes: Map<String, Double>)
    data class Houses(val system: HouseSystem, val cusps: List<Double>)
    data class Aspect(val type: Int, val p1: String, val p2: String, val angle: Double, val orb: Double)
    data class Orbs(val map: Map<Int, Double>)

    val defaultOrbs = Orbs(mapOf(0 to 6.0, 60 to 6.0, 90 to 6.0, 120 to 6.0, 180 to 6.0))

    // 可由 UI 設定：高緯度時 Placidus 回退策略（預設 ASC）
    @Volatile var highLatFallback: HighLatFallback = HighLatFallback.ASC

    // 診斷模式：輸出 cusp 與殘差，便於對照外部工具
    @Volatile var diagnostics: Boolean = false
    private fun diag(tag: String, msg: String) { if (diagnostics) Log.d(tag, msg) }

    // --- Helpers ---
    private fun norm360(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    // Regiomontanus：將赤道自 MC 起每 30° 等分，投影至黃道
    private fun computeRegiomontanusHouses(utcInstant: Instant, lat: Double, lon: Double): Houses {
        val mc = computeMcLong(utcInstant) ?: 0.0
        val obliq = runCatching {
            val astro = Class.forName("io.github.cosinekitty.astronomy.Astronomy")
            val timeCls = Class.forName("io.github.cosinekitty.astronomy.AstroTime")
            val time = timeCls.getConstructor(Instant::class.java).newInstance(utcInstant)
            astro.getMethod("Obliquity", timeCls).invoke(null, time) as Double
        }.getOrElse { 23.43704 }
        fun raToEcl(raDeg: Double): Double {
            val rad = Math.PI / 180.0
            val α = raDeg * rad
            val ε = obliq * rad
            val y = Math.sin(α) / Math.cos(ε)
            val x = Math.cos(α)
            val λ = Math.atan2(y, x) / rad
            return norm360(λ)
        }
        val raMc = eclToRaDecInternal(mc, obliq).first
        val cusps = ArrayList<Double>(12)
        // 10th house at MC
        val c10 = mc
        val c11 = raToEcl(norm360(raMc + 30.0))
        val c12 = raToEcl(norm360(raMc + 60.0))
        val c1 = raToEcl(norm360(raMc + 90.0))
        val c2 = raToEcl(norm360(raMc + 120.0))
        val c3 = raToEcl(norm360(raMc + 150.0))
        val c4 = norm360(c10 + 180.0)
        val c5 = norm360(c2 + 180.0)
        val c6 = norm360(c3 + 180.0)
        val c7 = norm360(c1 + 180.0)
        val c8 = norm360(c11 + 180.0)
        val c9 = norm360(c12 + 180.0)
        cusps.addAll(listOf(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12))
        return Houses(HouseSystem.PLACIDUS, cusps) // 作為 fallback 回傳型別仍標示使用中系統
    }

    private fun eclToRaDecInternal(lambda: Double, eps: Double): Pair<Double, Double> {
        val rad = Math.PI / 180.0
        val λ = lambda * rad
        val ε = eps * rad
        val sinλ = Math.sin(λ)
        val cosλ = Math.cos(λ)
        val α = Math.atan2(sinλ * Math.cos(ε), cosλ) / rad
        val δ = Math.asin(Math.sin(ε) * sinλ) / rad
        return norm360(α) to δ
    }
    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(norm360(a) - norm360(b))
        return min(d, 360.0 - d)
    }

    private val bodyNames = listOf("Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn")

    /**
     * 計算行星黃經：優先嘗試以 Astronomy Engine（反射）取得；若不可用則退回內建簡化模型。
     */
    fun computePlanets(utcInstant: Instant, lat: Double, lon: Double): PlanetPositions {
        // 反射載入 Astronomy Engine
        val engine = try { Class.forName("io.github.cosinekitty.astronomy.Astronomy") } catch (_: Throwable) { null }
        return if (engine != null) {
            runCatching {
                val astroTimeCls = Class.forName("io.github.cosinekitty.astronomy.AstroTime")
                val observerCls = Class.forName("io.github.cosinekitty.astronomy.Observer")
                val bodyCls = Class.forName("io.github.cosinekitty.astronomy.Body")
                val equatorEpochCls = Class.forName("io.github.cosinekitty.astronomy.EquatorEpoch")
                val aberrationCls = Class.forName("io.github.cosinekitty.astronomy.Aberration")

                val time = astroTimeCls.getConstructor(Instant::class.java).newInstance(utcInstant)
                val obs = observerCls.getConstructor(Double::class.javaPrimitiveType, Double::class.javaPrimitiveType, Double::class.javaPrimitiveType)
                    .newInstance(lat, lon, 0.0)

                val equatorMethod = engine.getMethod("Equator", bodyCls, astroTimeCls, observerCls, equatorEpochCls, aberrationCls)
                val eclipticMethod = engine.getMethod("Ecliptic", Class.forName("io.github.cosinekitty.astronomy.Equatorial"))

                val ofDate = equatorEpochCls.getField("OfDate").get(null)
                val corrected = aberrationCls.getField("Corrected").get(null)

                val result = mutableMapOf<String, Double>()
                val valueOf = bodyCls.getMethod("valueOf", String::class.java)
                for (name in bodyNames) {
                    val enumConst = valueOf.invoke(null, name)
                    val eq = equatorMethod.invoke(null, enumConst, time, obs, ofDate, corrected)
                    val ecl = eclipticMethod.invoke(null, eq)
                    val elon = ecl.javaClass.getField("elon").getDouble(ecl)
                    result[name] = norm360(elon)
                }
                PlanetPositions(result)
            }.getOrElse { fallbackPlanets(utcInstant) }
        } else {
            fallbackPlanets(utcInstant)
        }
    }

    // 簡化內建模型（舊版退場保險）
    private fun fallbackPlanets(utcInstant: Instant): PlanetPositions {
        val t = utcInstant.epochSecond.toDouble()
        val positions = mapOf(
            "Sun" to norm360((t / 24000.0) * 360.0),
            "Moon" to norm360((t / 3240.0) * 360.0),
            "Mercury" to norm360((t / 14000.0) * 360.0),
            "Venus" to norm360((t / 18000.0) * 360.0),
            "Mars" to norm360((t / 47000.0) * 360.0),
            "Jupiter" to norm360((t / 360000.0) * 360.0),
            "Saturn" to norm360((t / 900000.0) * 360.0)
        )
        return PlanetPositions(positions)
    }

    /**
     * Whole-Sign 宮位：
     * - WHOLE_SIGN_ASC：以上升點（Asc）為第一宮起點，等分 12 宮。
     * - WHOLE_SIGN_ARIES：以 0° 牡羊為第一宮起點，等分 12 宮（fallback）。
     */
    fun computeHouses(
        utcInstant: Instant,
        lat: Double,
        lon: Double,
        system: HouseSystem = HouseSystem.WHOLE_SIGN_ASC
    ): Houses {
        return when (system) {
            HouseSystem.WHOLE_SIGN_ARIES -> Houses(system, (0 until 12).map { i -> norm360(0.0 + i * 30.0) })
            HouseSystem.WHOLE_SIGN_ASC -> {
                val base = computeAscLong(utcInstant, lat, lon) ?: 0.0
                Houses(system, (0 until 12).map { i -> norm360(base + i * 30.0) })
            }
            HouseSystem.PLACIDUS -> computePlacidusHousesOrFallback(utcInstant, lat, lon)
        }
    }

    // Placidus（半弧分割嚴格版-數值近似）：以半日弧比例為目標，數值尋找各宮 cusp。
    private fun computePlacidusHousesOrFallback(utcInstant: Instant, lat: Double, lon: Double): Houses {
        val absLat = kotlin.math.abs(lat)
        if (absLat >= 66.0) {
            // 高緯度回退（ASC / REGIO 皆先回退到 Whole Sign，REGIO 可於後續升級）
            return when (highLatFallback) {
                HighLatFallback.ASC -> {
                    val asc = computeAscLong(utcInstant, lat, lon) ?: 0.0
                    val cuspsWs = (0 until 12).map { i -> norm360(asc + i * 30.0) }
                    Houses(HouseSystem.WHOLE_SIGN_ASC, cuspsWs)
                }
                HighLatFallback.REGIO -> {
                    computeRegiomontanusHouses(utcInstant, lat, lon)
                }
            }
        }
        // 取得 Asc, MC 與天文參數
        val asc = computeAscLong(utcInstant, lat, lon) ?: 0.0
        val astro = runCatching { Class.forName("io.github.cosinekitty.astronomy.Astronomy") }.getOrNull()
        val timeCls = runCatching { Class.forName("io.github.cosinekitty.astronomy.AstroTime") }.getOrNull()
        val time = try { timeCls?.getConstructor(Instant::class.java)?.newInstance(utcInstant) } catch (_: Throwable) { null }
        val lst = try { (astro?.getMethod("SiderealTime", timeCls)?.invoke(null, time) as Double) } catch (_: Throwable) { approxGmst(utcInstant, lon) }
        val obliq = try { (astro?.getMethod("Obliquity", timeCls)?.invoke(null, time) as Double) } catch (_: Throwable) { 23.43704 }
        val mc = computeMcLong(utcInstant) ?: norm360(asc + 90.0)

        // 以半弧比例數值尋找各宮 cusp：
        // 上半天：從 MC（HA=0）往東至地平線（Asc 上方或 Dc 上方）半弧 H，11=H/3，12=2H/3
        // 下半天：從 IC（HA=0）往東至地平線半弧 H，2=H/3，3=2H/3

        fun eclToRaDec(lambda: Double, eps: Double): Pair<Double, Double> {
            val rad = Math.PI / 180.0
            val λ = lambda * rad
            val ε = eps * rad
            val sinλ = Math.sin(λ)
            val cosλ = Math.cos(λ)
            val α = Math.atan2(sinλ * Math.cos(ε), cosλ) / rad // RA
            val δ = Math.asin(Math.sin(ε) * sinλ) / rad          // Dec
            return norm360(α) to δ
        }

        fun semiArc(latDeg: Double, decDeg: Double): Double {
            val rad = Math.PI / 180.0
            val φ = latDeg * rad
            val δ = decDeg * rad
            val cosH = (-Math.tan(φ) * Math.tan(δ)).coerceIn(-1.0, 1.0)
            val H = Math.acos(cosH) / rad // 0..180
            // 近極圈夾限，避免極端情況導致 H 幾乎為 0 或 180 度造成收斂不穩
            return H.coerceIn(1e-4, 179.9999)
        }

        fun hourAngle(lstDeg: Double, raDeg: Double): Double {
            var ha = lstDeg - raDeg
            ha = ((ha + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
            return Math.abs(ha)
        }

        fun raToEcl(raDeg: Double, eps: Double): Double {
            val rad = Math.PI / 180.0
            val α = raDeg * rad
            val ε = eps * rad
            val y = Math.sin(α) / Math.cos(ε)
            val x = Math.cos(α)
            val λ = Math.atan2(y, x) / rad
            return norm360(λ)
        }
        fun fOfRa(raDeg: Double, frac: Double): Double {
            val lambda = raToEcl(raDeg, obliq)
            val (_, dec) = eclToRaDec(lambda, obliq)
            val H = semiArc(lat, dec)
            val ha = hourAngle(lst, raDeg)
            return ha - H * frac
        }

        fun findCusp(targetFrac: Double, upperHemisphere: Boolean): Double {
            // 以 RA 為自變量進行根尋找：在對應半球的 MC/IC 周邊 ±90° 範圍內尋找變號
            val raMc = eclToRaDecInternal(mc, obliq).first
            val center = if (upperHemisphere) raMc else norm360(raMc + 180.0)
            val start = norm360(center - 90.0)
            val end = norm360(center + 90.0)
            fun inWindow(x: Double): Boolean {
                val a = start; val b = end
                return if (a <= b) (x >= a && x <= b) else (x >= a || x <= b)
            }
            var bestRa = center
            var bestAbs = 1e9
            val step = 1.0
            // 找第一個點作為 a
            var a = start
            if (!inWindow(a)) a = center
            var fa = fOfRa(a, targetFrac)
            fun bisection(a0: Double, b0: Double, fa0: Double, fb0: Double): Double {
                var left = a0; var right = b0
                var fl = fa0; var fr = fb0
                repeat(40) {
                    val mid = 0.5 * (left + right)
                    val fm = fOfRa(mid, targetFrac)
                    if (kotlin.math.abs(fm) < 1e-6) return mid
                    if (fl * fm <= 0) { right = mid; fr = fm } else { left = mid; fl = fm }
                }
                return 0.5 * (left + right)
            }
            // 順著窗口掃描一圈（最大 180° 範圍）
            var traversed = 0.0
            var ra = norm360(start + step)
            while (traversed <= 180.0 + 1e-6) {
                val fb = fOfRa(ra, targetFrac)
                val absb = kotlin.math.abs(fb)
                if (absb < bestAbs) { bestAbs = absb; bestRa = ra }
                if (fa.isFinite() && fb.isFinite() && fa * fb <= 0.0) {
                    var rootRa = bisection(ra - step, ra, fa, fb)
                    // Newton 精修（有限差分求導）
                    val h = 0.1
                    repeat(12) {
                        val fr0 = fOfRa(rootRa, targetFrac)
                        if (kotlin.math.abs(fr0) < 1e-8) return@repeat
                        val d = (fOfRa(rootRa + h, targetFrac) - fOfRa(rootRa - h, targetFrac)) / (2*h)
                        if (d == 0.0 || !d.isFinite()) return@repeat
                        var next = rootRa - fr0 / d
                        // 夾限避免跳出 [ra-step, ra]
                        val lo = norm360(ra - step); val hi = norm360(ra)
                        // 將 next 夾在當前半球窗口內
                        if (!inWindow(next)) next = center
                        if (next < lo || next > hi) next = (lo + hi) * 0.5
                        rootRa = next
                    }
                    val lambda = raToEcl(rootRa, obliq)
                    return norm360(lambda)
                }
                a = ra; fa = fb; ra = norm360(ra + step); traversed += step
            }
            // 若未找到變號，回退至最小殘差 RA，再換算 λ
            val lambda = raToEcl(bestRa, obliq)
            return norm360(lambda)
        }

        val c10 = mc
        val c1 = asc
        val c11 = findCusp(1.0 / 3.0, true)
        val c12 = findCusp(2.0 / 3.0, true)
        val c4 = norm360(c10 + 180.0)
        val c2 = findCusp(1.0 / 3.0, false)
        val c3 = findCusp(2.0 / 3.0, false)
        val c5 = norm360(c2 + 180.0)
        val c6 = norm360(c3 + 180.0)
        val c7 = norm360(c1 + 180.0)
        val c8 = norm360(c11 + 180.0)
        val c9 = norm360(c12 + 180.0)
        val cusps = listOf(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12)
        // 診斷輸出
        diag("AstroCalc", "PLACIDUS cusps: ${cusps.joinToString { String.format("%.3f", it) }}")
        // 檢算殘差：11/12/2/3 目標半弧分數
        fun resid(lambda: Double, frac: Double): Double {
            val (ra, _) = eclToRaDec(lambda, obliq)
            return kotlin.math.abs(fOfRa(ra, frac))
        }
        val r11 = resid(c11, 1.0/3.0); val r12 = resid(c12, 2.0/3.0)
        val r2 = resid(c2, 1.0/3.0); val r3 = resid(c3, 2.0/3.0)
        diag("AstroCalc", "Residuals deg: H11=${"%.4f".format(r11)}, H12=${"%.4f".format(r12)}, H2=${"%.4f".format(r2)}, H3=${"%.4f".format(r3)}")
        return Houses(HouseSystem.PLACIDUS, cusps)
    }

    // MC 黃經（反射使用 Astronomy Engine 的 Sidereal/Obliquity），失敗則用近似 GMST/固定 ε
    private fun computeMcLong(utcInstant: Instant): Double? = runCatching {
        val astroCls = Class.forName("io.github.cosinekitty.astronomy.Astronomy")
        val timeCls = Class.forName("io.github.cosinekitty.astronomy.AstroTime")
        val time = timeCls.getConstructor(Instant::class.java).newInstance(utcInstant)
        val sidereal = astroCls.getMethod("SiderealTime", timeCls).invoke(null, time) as Double
        val obliq = (astroCls.getMethod("Obliquity", timeCls).invoke(null, time) as Double)
        mcFromLstObliq(sidereal, obliq)
    }.getOrElse {
        val gmst = approxGmst(utcInstant, 0.0)
        val obliq = 23.43704
        mcFromLstObliq(gmst, obliq)
    }

    private fun mcFromLstObliq(lstDeg: Double, obliqDeg: Double): Double {
        val rad = Math.PI / 180.0
        val θ = lstDeg * rad
        val ε = obliqDeg * rad
        val y = Math.sin(θ)
        val x = Math.cos(θ) * Math.cos(ε)
        val λ = Math.atan2(y, x) / rad
        return norm360(λ)
    }

    // 將赤經/赤緯轉換為黃道黃經（度）
    private fun raDecToEclipticLong(raDeg: Double, decDeg: Double, obliqDeg: Double): Double {
        val rad = Math.PI / 180.0
        val α = raDeg * rad
        val δ = decDeg * rad
        val ε = obliqDeg * rad
        val y = Math.sin(α) * Math.cos(ε) + Math.tan(δ) * Math.sin(ε)
        val x = Math.cos(α)
        val λ = Math.atan2(y, x) / rad
        return norm360(λ)
    }

    // 估計給定緯度與赤緯的半日弧時角（度）（簡化近似）
    private fun calcSemiArcH(latDeg: Double, decDeg: Double): Double {
        val rad = Math.PI / 180.0
        val φ = latDeg * rad
        val δ = decDeg * rad
        val cosH = -Math.tan(φ) * Math.tan(δ)
        val H = kotlin.math.acos(cosH.coerceIn(-1.0, 1.0)) / rad // 0..180
        return H // 半日弧（度）
    }

    // 嘗試以 Astronomy Engine 反射計算上升點黃經；失敗則回傳 null
    private fun computeAscLong(utcInstant: Instant, lat: Double, lon: Double): Double? {
        return runCatching {
            val astroCls = Class.forName("io.github.cosinekitty.astronomy.Astronomy")
            val timeCls = Class.forName("io.github.cosinekitty.astronomy.AstroTime")
            val time = timeCls.getConstructor(Instant::class.java).newInstance(utcInstant)
            val sidereal = astroCls.getMethod("SiderealTime", timeCls).invoke(null, time) as Double
            val obliq = (astroCls.getMethod("Obliquity", timeCls).invoke(null, time) as Double)
            ascFromLstObliq(lat, sidereal, obliq)
        }.getOrElse {
            // 無法反射 Astronomy Engine：使用近似 GMST 與固定黃赤交角
            val gmst = approxGmst(utcInstant, lon)
            val obliq = 23.43704 // 近似黃赤交角（度）
            ascFromLstObliq(lat, gmst, obliq)
        }
    }

    // 以公式由本地恆星時與黃赤交角求 Asc 黃經
    private fun ascFromLstObliq(latDeg: Double, lstDeg: Double, obliqDeg: Double): Double {
        val rad = Math.PI / 180.0
        val φ = latDeg * rad
        val θ = lstDeg * rad
        val ε = obliqDeg * rad
        val y = -Math.cos(θ)
        val x = Math.sin(θ) * Math.cos(ε) + Math.tan(φ) * Math.sin(ε)
        val λ = Math.atan2(y, x) / rad
        return norm360(λ)
    }

    // 近似 GMST（度）；加入經度換算到本地恆星時（LST）
    private fun approxGmst(utcInstant: Instant, lonDeg: Double): Double {
        val jd = utcInstant.epochSecond / 86400.0 + 2440587.5 // Unix epoch to JD
        val T = (jd - 2451545.0) / 36525.0
        val gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * T * T - T * T * T / 38710000.0
        return norm360(gmst + lonDeg)
    }

    /**
     * 以真實黃經計算基礎相位（0/60/90/120/180）與容許度。
     */
    fun computeAspects(planets: PlanetPositions, orbs: Orbs = defaultOrbs): List<Aspect> {
        val keys = planets.longitudes.keys.toList()
        val result = mutableListOf<Aspect>()
        for (i in 0 until keys.size) for (j in i + 1 until keys.size) {
            val p1 = keys[i]
            val p2 = keys[j]
            val a1 = planets.longitudes[p1] ?: continue
            val a2 = planets.longitudes[p2] ?: continue
            val d = angleDiff(a1, a2)
            for (type in listOf(0, 60, 90, 120, 180)) {
                val orb = orbs.map[type] ?: continue
                val diff = abs(d - type)
                if (diff <= orb) {
                    result += Aspect(type, p1, p2, d, diff)
                    break
                }
            }
        }
        return result
    }

    /**
     * JSON 摘要（給測試/除錯）：包含行星、宮位與相位。
     */
    fun computeSummary(utcInstant: Instant, lat: Double, lon: Double): String {
        val planets = computePlanets(utcInstant, lat, lon)
        val houses = computeHouses(utcInstant, lat, lon)
        val aspects = computeAspects(planets)
        val planetsJson = JSONObject()
        planets.longitudes.forEach { (k, v) -> planetsJson.put(k, floor(v * 100.0) / 100.0) }
        val housesArr = JSONArray(houses.cusps.map { floor(it * 100.0) / 100.0 })
        val aspectsArr = JSONArray(aspects.map { a ->
            JSONObject().put("type", a.type).put("p1", a.p1).put("p2", a.p2)
                .put("angle", floor(a.angle * 100.0) / 100.0).put("orb", floor(a.orb * 100.0) / 100.0)
        })
        return JSONObject()
            .put("time", utcInstant.toString())
            .put("observer", JSONObject().put("lat", lat).put("lon", lon))
            .put("planets", planetsJson)
            .put("housesSystem", houses.system.name)
            .put("houses", housesArr)
            .put("aspects", aspectsArr)
            .put("note", "planets=EngineOrFallback; houses=WholeSign(ASC)")
            .toString()
    }
}
