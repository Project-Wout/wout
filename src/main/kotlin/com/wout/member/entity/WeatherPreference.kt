package com.wout.member.entity

import com.wout.common.entity.BaseTimeEntity
import com.wout.member.entity.enums.ReactionLevel
import jakarta.persistence.*
import org.hibernate.annotations.Comment
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * packageName    : com.wout.member.entity
 * fileName       : WeatherPreference
 * author         : MinKyu Park
 * date           : 2025-06-01
 * description    : 사용자 날씨 선호도 및 민감도 설정 엔티티 (더티체킹 패턴 적용)
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-06-01        MinKyu Park       최초 생성
 * 2025-06-08        MinKyu Park       더티체킹 패턴으로 변경 (2단계)
 */
@Entity
class WeatherPreference private constructor(
    /* ---------- PK / FK ---------- */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("날씨 선호도 설정 고유 ID")
    val id: Long = 0L,

    @Comment("회원 ID (FK)")
    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    /* ---------- Step-1 : 요소별 민감도 ---------- */
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_cold", length = 10, nullable = false)
    var reactionCold: ReactionLevel,

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_heat", length = 10, nullable = false)
    var reactionHeat: ReactionLevel,

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_humidity", length = 10, nullable = false)
    var reactionHumidity: ReactionLevel,

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_uv", length = 10, nullable = false)
    var reactionUv: ReactionLevel,

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_air", length = 10, nullable = false)
    var reactionAir: ReactionLevel,

    /* ---------- Step-2 : 체감 온도 기준 ---------- */
    @Comment("긴팔을 입기 시작하는 온도 (10–30℃)")
    @Column(name = "comfort_temperature", nullable = false)
    var comfortTemperature: Int = 20,

    /* ---------- Step-3 : 요소별 중요도(비중) ---------- */
    @Column(name = "importance_cold", nullable = false)
    var importanceCold: Double = 0.20,

    @Column(name = "importance_heat", nullable = false)
    var importanceHeat: Double = 0.20,

    @Column(name = "importance_humidity", nullable = false)
    var importanceHumidity: Double = 0.20,

    @Column(name = "importance_uv", nullable = false)
    var importanceUv: Double = 0.20,

    @Column(name = "importance_air", nullable = false)
    var importanceAir: Double = 0.20,

    /* ---------- 파생값 ---------- */
    @Column(name = "personal_temp_correction", nullable = false)
    var personalTempCorrection: Double = 0.0
) : BaseTimeEntity() {

    /* ===== Companion : 유틸 & 팩토리 ===== */
    companion object {
        private const val MIN_TEMP = 10
        private const val MAX_TEMP = 30
        private const val EPS = 0.02    // importance 합계 허용 오차(±2 %)

        /**
         * 온보딩 완료 시 **정적 팩토리** — 필수 파라미터 검증 후 객체 생성
         */
        fun createFromSetup(
            memberId: Long,
            rxCold: ReactionLevel, rxHeat: ReactionLevel,
            rxHumidity: ReactionLevel, rxUv: ReactionLevel, rxAir: ReactionLevel,
            comfortTemperature: Int,
            impCold: Double, impHeat: Double,
            impHumidity: Double, impUv: Double, impAir: Double
        ): WeatherPreference {
            require(memberId > 0) { "Member ID는 양수여야 합니다." }
            validateComfortTemperature(comfortTemperature)

            val balanced = balanceImportance(impCold, impHeat, impHumidity, impUv, impAir)
            val tempCorr = (comfortTemperature - 20) * 0.5   // 개인 온도 보정

            return WeatherPreference(
                memberId = memberId,
                reactionCold = rxCold, reactionHeat = rxHeat,
                reactionHumidity = rxHumidity, reactionUv = rxUv, reactionAir = rxAir,
                comfortTemperature = comfortTemperature,
                importanceCold = balanced[0], importanceHeat = balanced[1],
                importanceHumidity = balanced[2], importanceUv = balanced[3],
                importanceAir = balanced[4],
                personalTempCorrection = tempCorr
            )
        }

        /** 쾌적 온도 범위(10~30℃) 검증 */
        private fun validateComfortTemperature(v: Int) =
            require(v in MIN_TEMP..MAX_TEMP) {
                "쾌적 온도는 $MIN_TEMP~$MAX_TEMP℃ 사이여야 합니다."
            }

        /** 중요도 5값을 합계 1.0 ±2 % 로 자동 보정 */
        private fun balanceImportance(
            cold: Double, heat: Double, humi: Double, uv: Double, air: Double
        ): List<Double> {
            val list = mutableListOf(cold, heat, humi, uv, air)
            val diff = 1.0 - list.sum()
            if (kotlin.math.abs(diff) <= EPS) {
                list[list.lastIndex] = (list.last() + diff).coerceIn(0.0, 1.0)
            } else {
                throw IllegalArgumentException("importance 합계가 1.0에서 너무 벗어났습니다.")
            }
            return list
        }
    }

    /**
     *    사용자 설정을 부분 수정할 때 호출.
     *    null 파라미터는 기존 값을 유지하고, importance 값이 한 개라도 바뀌면
     *    다시 balanceImportance() 로 보정합니다.
     */
    fun updatePreferences(
        rxCold: ReactionLevel? = null, rxHeat: ReactionLevel? = null,
        rxHumidity: ReactionLevel? = null, rxUv: ReactionLevel? = null, rxAir: ReactionLevel? = null,
        comfortTemperature: Int? = null,
        impCold: Double? = null, impHeat: Double? = null,
        impHumidity: Double? = null, impUv: Double? = null, impAir: Double? = null
    ) {
        /* ① 민감도(Reaction) */
        rxCold?.let { reactionCold = it }
        rxHeat?.let { reactionHeat = it }
        rxHumidity?.let { reactionHumidity = it }
        rxUv?.let { reactionUv = it }
        rxAir?.let { reactionAir = it }

        /* ② 체감 온도 기준 */
        comfortTemperature?.let {
            validateComfortTemperature(it)
            this.comfortTemperature = it
            personalTempCorrection = (it - 20) * 0.5
        }

        /* ③ 중요도(Importance) */
        if (listOf(impCold, impHeat, impHumidity, impUv, impAir).any { it != null }) {
            val balanced = balanceImportance(
                impCold ?: importanceCold,
                impHeat ?: importanceHeat,
                impHumidity ?: importanceHumidity,
                impUv ?: importanceUv,
                impAir ?: importanceAir
            )
            importanceCold = balanced[0]; importanceHeat = balanced[1]
            importanceHumidity = balanced[2]; importanceUv = balanced[3]; importanceAir = balanced[4]
        }
    }

    /**
     * 중요도 맵(예: "cold" → 0.2) 반환
     */
    fun importanceMap(): Map<String, Double> = mapOf(
        "cold" to importanceCold,
        "heat" to importanceHeat,
        "humidity" to importanceHumidity,
        "uv" to importanceUv,
        "air" to importanceAir
    )

    /**
     * 체감 온도 계산<br>
     * ① Wind-Chill or ② Heat-Index 공식 적용 후<br>
     * ③ 사용자 보정치(personalTempCorrection) 와 습도 반응 보정 추가
     */
    fun calculateFeelsLikeTemperature(
        actual: Double, wind: Double, humidity: Double
    ): Double {
        var feels = actual
        if (actual <= 10 && wind >= 1.34) { // ① 추위 체감
            feels = 13.12 + 0.6215 * actual -
                    11.37 * wind.pow(0.16) +
                    0.3965 * actual * wind.pow(0.16)
        } else if (actual >= 27 && humidity >= 40) { // ② 더위 체감
            feels = calculateHeatIndex(actual, humidity)
        }
        return feels + personalTempCorrection + getHumidityCorrection(humidity) // ③ 개인 보정
    }

    /* ---------- 내부 보조 메서드 ---------- */

    /** Heat-Index(화씨)→섭씨 변환 */
    private fun calculateHeatIndex(tc: Double, h: Double): Double {
        val tf = tc * 9 / 5 + 32
        val hi = -42.379 + 2.04901523 * tf + 10.14333127 * h -
                0.22475541 * tf * h - 0.00683783 * tf * tf -
                0.05481717 * h * h + 0.00122874 * tf * tf * h +
                0.00085282 * tf * h * h - 0.00000199 * tf * tf * h * h
        return (hi - 32) * 5 / 9
    }

    /** 습도 민감도에 따른 추가 보정치 */
    private fun getHumidityCorrection(h: Double): Double {
        val base = when {
            h >= 85 -> 3.0
            h >= 75 -> 2.0
            h >= 65 -> 1.0
            h >= 40 -> 0.0
            h >= 30 -> -0.5
            else    -> -1.0
        }
        val mult = when (reactionHumidity) {
            ReactionLevel.HIGH   -> 1.5
            ReactionLevel.LOW    -> 0.5
            else                 -> 1.0
        }
        return base * mult
    }

    /* ---------- 디버그용 출력 ---------- */
    override fun toString(): String =
        "WeatherPreference(id=$id, member=$memberId, " +
                "Rx=[COLD:$reactionCold, HEAT:$reactionHeat, HUMI:$reactionHumidity, " +
                "UV:$reactionUv, AIR:$reactionAir], Comfort=$comfortTemperature, " +
                "Imp=${importanceMap().mapValues { (it.value * 100).roundToInt() }})"
}