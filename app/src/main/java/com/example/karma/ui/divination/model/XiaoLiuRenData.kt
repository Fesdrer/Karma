package com.example.karma.ui.divination.model

/**
 * 小六壬六宫索引（顺时针推算顺序）
 * 对应手指位置：食指根→食指尖→中指尖→无名指尖→无名指根→中指根
 */
enum class PalaceIndex(val index: Int, val label: String, val shortLabel: String) {
    DA_AN(0, "大安", "安"),
    LIU_LIAN(1, "留连", "连"),
    SU_XI(2, "速喜", "喜"),
    CHI_KOU(3, "赤口", "口"),
    XIAO_JI(4, "小吉", "吉"),
    KONG_WANG(5, "空亡", "亡"),
    ;

    companion object {
        fun fromIndex(index: Int): PalaceIndex =
            entries.first { it.index == ((index % 6) + 6) % 6 }
    }
}

/**
 * 吉凶等级
 */
enum class FortuneLevel(val label: String, val isAuspicious: Boolean, val color: Long) {
    GREAT_AUSPICIOUS("大吉", true, 0xFF69f0ae),
    AUSPICIOUS("小吉", true, 0xFFa0c4ff),
    INAUSPICIOUS("凶", false, 0xFFff5252),
    GREAT_INAUSPICIOUS("大凶", false, 0xFFff0000),
}

/**
 * 十二时辰
 */
enum class ShiChen(val index: Int, val label: String, val timeRange: String) {
    ZI(1, "子时", "23:00-01:00"),
    CHOU(2, "丑时", "01:00-03:00"),
    YIN(3, "寅时", "03:00-05:00"),
    MAO(4, "卯时", "05:00-07:00"),
    CHEN(5, "辰时", "07:00-09:00"),
    SI(6, "巳时", "09:00-11:00"),
    WU(7, "午时", "11:00-13:00"),
    WEI(8, "未时", "13:00-15:00"),
    SHEN(9, "申时", "15:00-17:00"),
    YOU(10, "酉时", "17:00-19:00"),
    XU(11, "戌时", "19:00-21:00"),
    HAI(12, "亥时", "21:00-23:00"),
    ;

    companion object {
        fun fromHour(hour: Int): ShiChen = when (hour) {
            in 23..23, in 0..0 -> ZI
            in 1..2 -> CHOU
            in 3..4 -> YIN
            in 5..6 -> MAO
            in 7..8 -> CHEN
            in 9..10 -> SI
            in 11..12 -> WU
            in 13..14 -> WEI
            in 15..16 -> SHEN
            in 17..18 -> YOU
            in 19..20 -> XU
            in 21..22 -> HAI
            else -> ZI
        }

        fun fromIndex(index: Int): ShiChen =
            entries.first { it.index == ((index - 1) % 12 + 12) % 12 + 1 }
    }
}

/**
 * 单宫完整启示数据
 */
data class PalaceRevelation(
    val index: PalaceIndex,
    val name: String,
    val fortuneLevel: FortuneLevel,
    val wuxing: String,
    val direction: String,
    val color: String,
    val sixGods: String,
    val verse: String,
    val fortune: String,
    val wealth: String,
    val love: String,
    val career: String,
    val health: String,
)

/**
 * 小六壬六宫完整数据（硬编码传统文化知识）
 */
object XiaoLiuRenPalaces {

    val allPalaces: List<PalaceRevelation> = listOf(
        PalaceRevelation(
            index = PalaceIndex.DA_AN,
            name = "大安",
            fortuneLevel = FortuneLevel.GREAT_AUSPICIOUS,
            wuxing = "木",
            direction = "东方",
            color = "青色",
            sixGods = "青龙",
            verse = "大安事事昌，求财在坤方。\n失物去不远，宅舍保安康。\n行人身未动，病者主无妨。\n将军回田野，仔细更推详。",
            fortune = "运势稳定成长，诸事不宜躁进，宜静不宜动。当前处于平稳期，保持现状即可见好，强行推进反而不利。",
            wealth = "宜守旧业，不宜盲目扩张。财运稳定，但无大惊喜。求财宜往坤方（西南方向），可得贵人相助。",
            love = "感情稳定顺遂。女子问卦尤为吉利，感情生活和谐美满。男子问卦则感情平淡但无波澜，宜多主动。",
            career = "工作稳定，易得领导赏识。不宜主动求变，静待时机更为明智。专注于本职工作可稳步上升。",
            health = "身体无大碍，注意日常操劳积累。保持良好的作息即可，偶有小恙自行痊愈。",
        ),
        PalaceRevelation(
            index = PalaceIndex.LIU_LIAN,
            name = "留连",
            fortuneLevel = FortuneLevel.INAUSPICIOUS,
            wuxing = "水",
            direction = "北方",
            color = "黑色",
            sixGods = "玄武",
            verse = "留连事难成，求谋日未明。\n官事只宜缓，去者未回程。\n失物南方见，急讨方心称。\n更须防口舌，人口且平平。",
            fortune = "运势低迷，凡事受阻，心情不开朗。当前时机未到，强行推动只会徒增烦恼，宜守不宜动。",
            wealth = "求财难得，有破财之象。投资理财须格外谨慎，不宜任何冒险行为。守成为上策。",
            love = "感情沟通不良，易陷入冷战或误解。双方各自坚持已见，需要耐心等待转机。不宜在此时做重大决定。",
            career = "事业遇小人扯后腿，项目进展缓慢。注意同事间的暗流，少说话多做事。等待风头过去再图发展。",
            health = "肠胃不适或精神压力较大。注意饮食规律，避免过度焦虑。适当运动可缓解压力。",
        ),
        PalaceRevelation(
            index = PalaceIndex.SU_XI,
            name = "速喜",
            fortuneLevel = FortuneLevel.AUSPICIOUS,
            wuxing = "火",
            direction = "南方",
            color = "红色",
            sixGods = "朱雀",
            verse = "速喜喜来临，求财向南行。\n失物申未午，逢人路上寻。\n官事有福德，病者无祸侵。\n田宅六畜吉，行人有信音。",
            fortune = "喜讯将至，运势渐开。积极行动可如愿得偿，时机已到不宜犹豫。向阳而动，南方大吉。",
            wealth = "求财可得，但需注意先破后得之象。向南发展有利，主动出击比被动等待更容易获得回报。",
            love = "新恋火热甜蜜，感情迅速升温。但相处日久则可能有口舌之争，需注意沟通方式。单身者桃花将至。",
            career = "工作得利，项目推进顺利。注意文件疏失和细节管理。适合主动争取机会，努力会有好的回报。",
            health = "注意心脏、血液循环方面的问题。问题不大但需关注，保持规律运动。避免熬夜伤身。",
        ),
        PalaceRevelation(
            index = PalaceIndex.CHI_KOU,
            name = "赤口",
            fortuneLevel = FortuneLevel.INAUSPICIOUS,
            wuxing = "金",
            direction = "西方",
            color = "白色",
            sixGods = "白虎",
            verse = "赤口主口舌，官非切要防。\n失物速速讨，行人有惊慌。\n六畜多作怪，病者出西方。\n更须防咀咒，诚恐染瘟殃。",
            fortune = "运势不明，口舌是非最多。大事宜快办不宜拖延，小事能忍则忍。切忌与人争执，避免官非诉讼。",
            wealth = "财运大起大落，求财不易。投资理财须格外谨慎，防范损失。不宜借贷或为人担保。",
            love = "感情纷争较多，女方尤其注意身体健康。双方情绪易激动，小事化大。需要冷静沟通，避免言语伤害。",
            career = "武职或粗重行业较为顺利，文职工作者易有口舌不顺。注意职场中的人际关系，小心被人背后中伤。",
            health = "注意胸口、支气管方面的问题。有血光之灾的暗示，出行在外多加小心。建议体检排查隐患。",
        ),
        PalaceRevelation(
            index = PalaceIndex.XIAO_JI,
            name = "小吉",
            fortuneLevel = FortuneLevel.GREAT_AUSPICIOUS,
            wuxing = "木",
            direction = "东方",
            color = "青色",
            sixGods = "六合",
            verse = "小吉最吉昌，路上好商量。\n阴人来报喜，失物在坤方。\n行人即便至，交关甚是强。\n凡事皆和合，病者叩穹苍。",
            fortune = "六合和合之象，保持现状就会越来越好。凡事皆顺遂，出门在外也有好运气。女性有报喜之兆。",
            wealth = "求财可得，有因人得财之兆。合作关系带来收益，西南方向发展有利。宜与他人合作共赢。",
            love = "无感情者可由人介绍结识良缘。有感情者关系顺利和谐。女性缘分尤其佳，有喜事临门之兆。",
            career = "注意财务管理和下属沟通。合作项目顺利，团队协作良好。适合推进需要多方协调的事务。",
            health = "肝胆、消化系统可能有小问题，注意饮食健康。整体无大碍，叩谢上天保佑即可安心。",
        ),
        PalaceRevelation(
            index = PalaceIndex.KONG_WANG,
            name = "空亡",
            fortuneLevel = FortuneLevel.GREAT_INAUSPICIOUS,
            wuxing = "土",
            direction = "中央",
            color = "黄色",
            sixGods = "勾陈",
            verse = "空亡事不祥，阴人多乖张。\n求财无利益，行人有灾殃。\n失物寻不见，官事有刑伤。\n病人逢暗鬼，解禳保安康。",
            fortune = "六宫中最凶，事情多无结果，内心不安无所适从。此时不宜做任何重大决定，宜静心等待此运过去。",
            wealth = "求财难得，保守为要。投资理财几乎无利可图，还可能遭遇损失。守住现有的就是最好的结果。",
            love = "感情争执多，可能有第三方介入的迹象。双方信任出现裂痕，需要时间和诚心来修复。不宜做出冲动决定。",
            career = "工作失利，有被陷害或暗算的风险。行事须格外小心，不要轻信他人。项目可能因外部因素受阻。",
            health = "脾胃、神经系统问题为主。甚至可能有灵界致病因的说法。建议祈福解禳，同时重视身体检查。",
        ),
    )

    fun getPalace(index: PalaceIndex): PalaceRevelation =
        allPalaces.first { it.index == index }

    fun getPalaceByIndex(idx: Int): PalaceRevelation =
        getPalace(PalaceIndex.fromIndex(idx))
}

/**
 * 农历日历辅助工具
 *
 * 支持公历 → 农历转换（1900–2100 年），用于小六壬传统模式自动填充当前月日时辰。
 */
object LunarCalendarHelper {

    /** 农历月份名称映射 */
    val monthNames = listOf(
        "", "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月",
    )

    /** 农历日期名称映射（1-30） */
    val dayNames = listOf(
        "", "初一", "初二", "初三", "初四", "初五",
        "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五",
        "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五",
        "廿六", "廿七", "廿八", "廿九", "三十",
    )

    /** 农历日期 */
    data class LunarDate(val year: Int, val month: Int, val day: Int, val isLeap: Boolean)

    // ====== 公历 → 农历（使用 Android 内置 ICU ChineseCalendar） ======

    /** 将公历日期转为农历日期（支持 1900-2100 年范围）
     *
     *  使用 android.icu.util.ChineseCalendar（API 24+）实现，
     *  可靠性优于手写查表算法，且时区、闰月处理更准确。
     */
    fun solarToLunar(year: Int, month: Int, day: Int): LunarDate {
        // 使用中午 12:00 避免午夜跨日时区边界导致农历日差一天
        val gregCal = java.util.GregorianCalendar(year, month - 1, day)
        gregCal.set(java.util.Calendar.HOUR_OF_DAY, 12)
        gregCal.set(java.util.Calendar.MINUTE, 0)
        gregCal.set(java.util.Calendar.SECOND, 0)
        gregCal.set(java.util.Calendar.MILLISECOND, 0)

        val chineseCal = android.icu.util.ChineseCalendar()
        chineseCal.timeInMillis = gregCal.timeInMillis

        val extendedYear = chineseCal.get(android.icu.util.ChineseCalendar.EXTENDED_YEAR)
        // CHINESE_EPOCH_YEAR = 2637, EXTENDED_YEAR 基于此偏移
        val lunarYear = extendedYear - 2637
        val lunarMonth = chineseCal.get(java.util.Calendar.MONTH) + 1
        val lunarDay = chineseCal.get(java.util.Calendar.DAY_OF_MONTH)
        val isLeap = chineseCal.get(android.icu.util.ChineseCalendar.IS_LEAP_MONTH) == 1

        return LunarDate(lunarYear, lunarMonth, lunarDay, isLeap)
    }

    /** 获取当前时刻的农历日期 */
    fun getCurrentLunarDate(): LunarDate {
        val cal = java.util.Calendar.getInstance()
        return solarToLunar(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        )
    }

    /** 获取当前时刻的时辰 */
    fun getCurrentShiChen(): ShiChen {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return ShiChen.fromHour(hour)
    }

    /** 根据 24 小时制获取对应时辰 */
    fun getShiChenFromHour(hour: Int): ShiChen = ShiChen.fromHour(hour)

}
