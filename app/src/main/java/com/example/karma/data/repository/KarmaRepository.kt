package com.example.karma.data.repository

import com.example.karma.data.local.dao.HistoryEntryDao
import com.example.karma.data.local.dao.KarmaSettingsDao
import com.example.karma.data.local.entity.Bet
import com.example.karma.data.local.entity.HistoryEntryEntity
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.model.Fraction
import com.example.karma.data.model.Rank
import com.example.karma.data.model.TimerState
import com.example.karma.data.model.TimerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class KarmaRepository(
    private val historyDao: HistoryEntryDao,
    private val settingsDao: KarmaSettingsDao,
) {
    companion object {
        private const val MAX_HISTORY = 2000

        /** 将时间戳格式化为 "yyyy-MM-dd"。供外部复用。 */
        fun formatDate(timestamp: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            return String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH))
        }
    }

    // ---- History ----

    val allHistory: Flow<List<HistoryEntryEntity>> = historyDao.getAllEntries()

    suspend fun addHistoryEntry(
        delta: Float,
        event: String,
        type: String,
    ): HistoryEntryEntity {
        val current = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        val newTotal = roundToOneDecimal(current.totalScore + delta)
        val entry = HistoryEntryEntity(
            timestamp = System.currentTimeMillis(),
            delta = roundToOneDecimal(delta),
            event = event,
            type = type,
            totalAfter = newTotal,
        )
        settingsDao.upsertSettings(current.copy(totalScore = newTotal))
        val id = historyDao.insertEntry(entry)

        // Trim history if over limit
        val count = historyDao.getCount()
        val excess = count - MAX_HISTORY
        if (excess > 0) {
            historyDao.trimOldest(excess)
        }

        return entry.copy(id = id)
    }

    // ---- Settings ----

    val settings: Flow<KarmaSettingsEntity> = settingsDao.getSettings().map {
        // 事件默认分数与名称列表长度对齐（旧库/导入数据缺列时补 ±1 默认值）
        (it ?: KarmaSettingsEntity()).withNormalizedEventScores()
    }

    suspend fun getTotalScoreOnce(): Float {
        return settingsDao.getSettingsOnce()?.totalScore ?: 0f
    }

    suspend fun updateScorePresets(presets: List<Float>) {
        settingsDao.updateScorePresets(presets)
    }

    suspend fun updateGoodDeedPresets(presets: List<String>) {
        settingsDao.updateGoodDeedPresets(presets)
    }

    suspend fun updateBadDeedPresets(presets: List<String>) {
        settingsDao.updateBadDeedPresets(presets)
    }

    suspend fun updateGoodResultPresets(presets: List<String>) {
        settingsDao.updateGoodResultPresets(presets)
    }

    suspend fun updateAllSettings(settings: KarmaSettingsEntity) {
        settingsDao.upsertSettings(settings)
    }

    /**
     * 只更新衰减开关与上次扣除日期，不触碰其他设置列。
     * 供设置页打开衰减开关时使用：仅持久化衰减字段，
     * 避免把「未点保存」的设置草稿（阶位增删/事件编辑等）整个写入 DB。
     */
    suspend fun updateDecayFields(decayEnabled: Boolean, lastDecayDate: String) {
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        settingsDao.upsertSettings(settings.copy(
            decayEnabled = decayEnabled,
            lastDecayDate = lastDecayDate,
        ))
    }

    // ---- Daily Must-Do ----

    /** 将指定 deed 的 vis 标记为 1（已做），同时记录 lastDate=今天。 */
    suspend fun markDeedDone(deedName: String) {
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        val updatedDeeds = settings.dailyMustDoDeeds.map {
            if (it.name == deedName) it.copy(vis = 1) else it
        }
        val todayStr = formatDate(System.currentTimeMillis())
        settingsDao.upsertSettings(settings.copy(
            dailyMustDoDeeds = updatedDeeds,
            dailyMustDoLastDate = todayStr,
        ))
    }

    /** 将所有 deed 的 vis 重置为 0（新的一天/配置变更时调用）。 */
    suspend fun resetDailyMustDoVis() {
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        val resetDeeds = settings.dailyMustDoDeeds.map { it.copy(vis = 0) }
        val todayStr = formatDate(System.currentTimeMillis())
        settingsDao.upsertSettings(settings.copy(
            dailyMustDoDeeds = resetDeeds,
            dailyMustDoLastDate = todayStr,
        ))
    }

    // ---- 誓约 ----

    /** 立下誓约：写入 bets 列表 + 历史记录（分数不变）。 */
    suspend fun addBet(bet: Bet) {
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        settingsDao.upsertSettings(settings.copy(bets = settings.bets + bet))
        addHistoryEntry(
            delta = 0f,
            // 四项分 4 行显示，不用分隔符（历史显示支持换行，祈福记录已有 \n 先例）
            event = "誓约：${bet.content}\n${bet.deadline}\n+${formatPoints(bet.successPoints)}\n-${formatPoints(bet.failurePoints)}",
            type = "bet",
        )
    }

    /** 了结誓约：success=true 加分，false 减分；从列表移除；写结果记录。 */
    suspend fun resolveBet(bet: Bet, success: Boolean) {
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        // 防重复：快速连点 ✔/× 时，第二次点击可能发生在 Flow 刷新前，
        // 若该誓约已不在列表（已了结）则直接忽略，避免重复加减分。
        if (bet !in settings.bets) return
        val delta = if (success) bet.successPoints else -bet.failurePoints
        settingsDao.upsertSettings(settings.copy(bets = settings.bets.filter { it != bet }))
        addHistoryEntry(
            delta = delta,
            event = "誓约结果：${bet.content}\n${bet.deadline}\n${if (success) "成功" else "失败"}",
            type = "bet_result",
        )
    }

    // ---- Timer Persistence ----

    /** 保存计时器状态到 Room — 只更新 timer 相关字段，不碰其他列，避免并发写覆盖。 */
    suspend fun saveTimerState(
        status: String,
        startElapsed: Long,
        resumeElapsed: Long,
        accumulatedMs: Long,
        selectedScore: Float,
        selectedEvent: String,
    ) {
        settingsDao.updateTimerFields(
            timerStatus = status,
            timerStartElapsed = startElapsed,
            timerResumeElapsed = resumeElapsed,
            timerAccumulatedMs = accumulatedMs,
            timerSelectedScore = selectedScore,
            timerSelectedEvent = selectedEvent,
        )
    }

    /** 从 Room 读取计时器状态，返回 null 表示无计时 */
    suspend fun loadTimerState(): TimerState? {
        val s = settingsDao.getSettingsOnce() ?: return null
        if (s.timerStatus == "IDLE") return null
        return TimerState(
            status = try { TimerStatus.valueOf(s.timerStatus) } catch (_: Exception) { TimerStatus.IDLE },
            startElapsed = s.timerStartElapsed,
            resumeElapsed = s.timerResumeElapsed,
            accumulatedMs = s.timerAccumulatedMs,
            selectedScore = s.timerSelectedScore,
            selectedEvent = s.timerSelectedEvent,
        )
    }

    /** 清除 Room 中的计时状态（只清 timer 字段，不碰其他列）。 */
    suspend fun clearTimerState() {
        settingsDao.updateTimerFields(
            timerStatus = "IDLE",
            timerStartElapsed = 0L,
            timerResumeElapsed = 0L,
            timerAccumulatedMs = 0L,
            timerSelectedScore = 0f,
            timerSelectedEvent = "",
        )
    }

    // ---- Decay ----

    /**
     * 业力衰减：检查并补扣。
     * 返回本次扣除的总分数（0 表示未扣）。
     *
     * 新算法：
     * - lastDecayDate 为空 → 设为今天并返回 0（不做追溯扣除）
     * - 日期B = 今天，若当前时间 ≥ 设定时间则为明天
     * - while 日期A < 日期B：按阶位扣分，时间戳统一为当前时间
     */
    suspend fun applyDecay(): Float {
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        if (!settings.decayEnabled) return 0f

        val now = System.currentTimeMillis()
        val todayStr = KarmaRepository.formatDate(now)

        // lastDecayDate 为空 → 设置今天并返回 0，不做追溯扣除
        if (settings.lastDecayDate.isEmpty()) {
            settingsDao.upsertSettings(settings.copy(lastDecayDate = todayStr))
            return 0f
        }

        // 确定日期B：若当前时间 >= 设定时间 → 日期B=明天，否则日期B=今天
        val calB = Calendar.getInstance()
        val currentHour = calB.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calB.get(Calendar.MINUTE)
        if (currentHour > settings.decayHour ||
            (currentHour == settings.decayHour && currentMinute >= settings.decayMinute)
        ) {
            calB.add(Calendar.DAY_OF_YEAR, 1)
        }
        val dateB = KarmaRepository.formatDate(calB.timeInMillis)

        // 日期A >= 日期B → 不需要扣除
        if (settings.lastDecayDate >= dateB) return 0f

        // 计算天数差
        fun dateToCal(dateStr: String): Calendar {
            val parts = dateStr.split("-")
            return Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val calA = dateToCal(settings.lastDecayDate)
        val calBstart = dateToCal(dateB)
        val diffMs = calBstart.timeInMillis - calA.timeInMillis
        val daysToCatchUp = (diffMs / (24L * 60 * 60 * 1000L)).toInt()
        if (daysToCatchUp <= 0) return 0f

        var totalDeducted = 0f
        var currentScore = settings.totalScore
        // 时间戳统一为"此刻"，所有补扣记录相同
        val uniformTimestamp = System.currentTimeMillis()

        // ---- 每日必做：每个 deed 有自己的 name/penalty/vis ----
        val dailyDeeds = settings.dailyMustDoDeeds

        repeat(daysToCatchUp) {
            // --- 业力衰减扣分（业力 ≤ 0 不衰减；不钳制：正数少于衰减量时允许扣成负数，之后因业力为负停止衰减） ---
            if (currentScore > 0f) {
                val rank = getDecayRank(currentScore, settings.rankThresholds)
                val deduction = getDecayAmountForRank(rank, settings.rankDecayAmounts)
                if (deduction > 0f) {
                    currentScore = roundToOneDecimal(currentScore - deduction)
                    totalDeducted += deduction
                    historyDao.insertEntry(
                        HistoryEntryEntity(
                            timestamp = uniformTimestamp,
                            delta = -deduction,
                            event = "业力衰减",
                            type = "decay",
                            totalAfter = currentScore,
                        )
                    )
                }
            }

            // --- 每日必做扣分（step 2：每个 deed 有自己的 vis，vis=1 跳过，else 扣分） ---
            for (deed in dailyDeeds) {
                if (deed.vis == 1) continue  // vis=1 → 已做，跳过
                if (deed.penalty > 0f) {
                    currentScore = roundToOneDecimal(currentScore - deed.penalty)
                    totalDeducted += deed.penalty
                    historyDao.insertEntry(
                        HistoryEntryEntity(
                            timestamp = uniformTimestamp,
                            delta = -deed.penalty,
                            event = "未完成：${deed.name}",
                            type = "daily_must_do",
                            totalAfter = currentScore,
                        )
                    )
                }
            }
        }

        // ---- step 3：循环完成后，需补扣天数>0 → 所有 deed 的 vis 重置为 0（新的一天） ----
        val newDeeds = if (daysToCatchUp > 0) {
            dailyDeeds.map { it.copy(vis = 0) }
        } else {
            dailyDeeds
        }

        // 更新总分、lastDecayDate、每日必做 deeds（每个 deed 有自己的 name/penalty/vis）
        settingsDao.upsertSettings(settings.copy(
            totalScore = currentScore,
            lastDecayDate = dateB,
            dailyMustDoDeeds = newDeeds,
        ))

        return totalDeducted
    }

    /**
     * 根据分数确定阶位（1~9）。
     * 负数归为一阶。
     */
    private fun getDecayRank(score: Float, thresholds: List<Float>): Int {
        val t = thresholds
        return when {
            score < 0 -> 1
            t.isNotEmpty() && score < t[0] -> 1
            t.size > 1 && score < t[1] -> 2
            t.size > 2 && score < t[2] -> 3
            t.size > 3 && score < t[3] -> 4
            t.size > 4 && score < t[4] -> 5
            t.size > 5 && score < t[5] -> 6
            t.size > 6 && score < t[6] -> 7
            t.size > 7 && score < t[7] -> 8
            else -> 9
        }
    }

    /**
     * 根据阶位从配置中取扣除量。
     */
    private fun getDecayAmountForRank(rank: Int, amounts: List<Float>): Float {
        if (amounts.isEmpty()) return 2f  // 默认 2
        val index = (rank - 1).coerceIn(0, amounts.size - 1)
        return amounts[index]
    }

    // ---- Rank ----

    fun getRank(score: Float, settings: KarmaSettingsEntity): Rank? {
        if (score < 0f) {
            // 负数阶位：阈值降序 ts=[-10,-20,-30,…,-80]（数量随设置页增删变化）
            // (-10,0]→-1，(-20,-10]→-2，…，最后一个阈值以下→最深一级
            val ts = settings.negativeRankThresholds
            val names = settings.negativeRankNames
            // 动态级数：score > ts[i] → 第 i+1 级；都不满足 → 最深一级
            var level = names.size.coerceAtLeast(1)
            for (i in ts.indices) {
                if (score > ts[i]) {
                    level = i + 1
                    break
                }
            }
            return Rank(
                min = if (level == names.size) -Float.MAX_VALUE else ts.getOrElse(level - 1) { -10f },
                max = if (level == 1) 0f else ts.getOrElse(level - 2) { -10f },
                level = -level,
                name = names.getOrElse(level - 1) { "?" },
                colorHex = settings.negativeRankColors.getOrElse(level - 1) { 0xFF000000L },
            )
        }
        val ranks = Rank.listFrom(settings.rankThresholds, settings.rankNames, settings.rankColors)
        return ranks.find { score >= it.min && score < it.max }
    }

    fun buildRanks(settings: KarmaSettingsEntity): List<Rank> {
        return Rank.listFrom(settings.rankThresholds, settings.rankNames, settings.rankColors)
    }

    /** 负数阶位色带列表（用于数轴负数区域与徽章，颜色随设置）。 */
    fun buildNegativeRanks(settings: KarmaSettingsEntity): List<Rank> {
        val ts = settings.negativeRankThresholds
        val names = settings.negativeRankNames
        val colors = settings.negativeRankColors
        return names.indices.map { i ->
            val level = i + 1
            Rank(
                min = if (level == names.size) -Float.MAX_VALUE else ts.getOrElse(level - 1) { -10f },
                max = if (level == 1) 0f else ts.getOrElse(level - 2) { -10f },
                level = -level,
                name = names.getOrElse(i) { "?" },
                colorHex = colors.getOrElse(i) { 0xFF000000L },
            )
        }
    }

    // ---- 占卜每日次数限制 ----

    /** 当前阶位对应的每日占卜次数上限（每阶一个值；负阶查 negativeRankDivinationLimits，默认全 0）。 */
    fun getDivinationLimit(settings: KarmaSettingsEntity): Int {
        val level = getRank(settings.totalScore, settings)?.level ?: 1
        return if (level > 0) {
            settings.rankDivinationLimits.getOrElse(level - 1) { 2 }
        } else {
            settings.negativeRankDivinationLimits.getOrElse(-level - 1) { 0 }
        }
    }

    /** 今日剩余可占卜次数（按 divinationDate 判断是否跨天重置）。气运测试不计入。 */
    fun getDivinationRemaining(settings: KarmaSettingsEntity): Int {
        val today = formatDate(System.currentTimeMillis())
        val count = if (settings.divinationDate == today) settings.divinationCount else 0
        return (getDivinationLimit(settings) - count).coerceAtLeast(0)
    }

    /** 记录一次占卜（进入占卜即计一次，扣分在占卜完成时另记）。 */
    suspend fun recordDivination() {
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        val today = formatDate(System.currentTimeMillis())
        settingsDao.upsertSettings(settings.copy(
            divinationDate = today,
            divinationCount = if (settings.divinationDate == today) settings.divinationCount + 1 else 1,
        ))
    }

    /** 清空全部历史记录（选择性重置：勾选「历史记录」分组时在保存时调用）。 */
    suspend fun clearAllHistory() {
        historyDao.deleteAll()
    }

    // ---- Import / Export ----

    suspend fun exportJson(): String {
        val gson = com.google.gson.Gson()
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        val history = historyDao.getAllEntriesList()

        val root = com.google.gson.JsonObject()
        root.addProperty("version", 2)
        root.addProperty("totalScore", settings.totalScore.toDouble())
        root.add("settings", gson.toJsonTree(settings))

        val historyArray = com.google.gson.JsonArray()
        for (entry in history) {
            val obj = com.google.gson.JsonObject()
            obj.addProperty("id", entry.id)
            obj.addProperty("timestamp", entry.timestamp)
            obj.addProperty("delta", entry.delta.toDouble())
            obj.addProperty("event", entry.event)
            obj.addProperty("type", entry.type)
            obj.addProperty("totalAfter", entry.totalAfter.toDouble())
            historyArray.add(obj)
        }
        root.add("history", historyArray)

        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)
    }

    /**
     * 导入数据。
     * @return null 表示导入成功；非 null 为失败原因（直接展示给用户）。
     */
    suspend fun importData(raw: String, format: String): String? {
        return try {
            val ok = if (format == "json") importJson(raw) else importCsv(raw)
            if (ok) {
                null
            } else if (format == "json") {
                "JSON 格式不正确或版本不兼容（需要 version=2 的导出文件）"
            } else {
                "CSV 格式不正确"
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            "JSON 格式错误，文件可能损坏或被截断：${e.message?.trim()?.take(80) ?: ""}"
        } catch (e: Exception) {
            "解析失败：${e.message?.trim()?.take(80) ?: "未知错误"}"
        }
    }

    private suspend fun importJson(raw: String): Boolean {
        val gson = com.google.gson.Gson()

        // 使用 JsonParser 直接解析，保留数字原始类型（避免 Map 中间步骤把 Long/Int 变成 Double）
        val root = com.google.gson.JsonParser.parseString(raw).asJsonObject

        // v2 格式校验：无 version 或 version≠2 → 拒绝（不兼容旧版本）
        if (root.get("version")?.asInt != 2) return false

        // v2 格式：settings 键存在 → 整体替换全部设置
        val settingsElement = root.get("settings") ?: return false
        val settingsObj = settingsElement.asJsonObject
        // 乘法按钮（v4.0，Fraction 存储）：先摘出该字段手动解析，兼容两种 JSON 格式——
        // 新格式对象数组 [{"numerator":1,"denominator":3}] 与旧 v20 格式 Float 数组 [0.33333334]，
        // 避免 gson 直接反序列化 List<Fraction> 时遇到旧 Float 数组抛 JsonSyntaxException 导致整次导入失败。
        val multiplierJson = settingsObj.remove("multiplierPresets")
        val importedSettings = gson.fromJson(settingsObj, KarmaSettingsEntity::class.java)
        val importedMultipliers = if (multiplierJson != null && multiplierJson.isJsonArray) {
            multiplierJson.asJsonArray.mapNotNull { el ->
                when {
                    el.isJsonObject -> {
                        val obj = el.asJsonObject
                        Fraction.of(
                            obj.get("numerator")?.asLong ?: 0L,
                            obj.get("denominator")?.asLong ?: 0L,
                        )
                    }
                    el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> Fraction.fromFloat(el.asFloat)
                    else -> null
                }
            }
        } else {
            // 旧导出无该字段（v4.0 之前）：补默认乘数，保证主页面乘法按钮可用
            KarmaSettingsEntity().multiplierPresets
        }
        // 导入后重置所有每日必做 vis=0（导入是全新开始，不应保留旧的 vis 状态）
        val resetDeeds = gsonNullable(importedSettings.dailyMustDoDeeds)?.map { it.copy(vis = 0) } ?: emptyList()
        // 列表字段兜底：gson 用 unsafe 分配实例绕过构造函数（默认值不生效），
        // 缺键时列表运行时为 null，写 NULL 到 NOT NULL 列会抛异常导致整个导入失败，这里补空列表。
        settingsDao.upsertSettings(importedSettings.copy(
            dailyMustDoDeeds = resetDeeds,
            bets = gsonNullable(importedSettings.bets) ?: emptyList(),
            negativeRankNames = gsonNullable(importedSettings.negativeRankNames) ?: emptyList(),
            negativeRankThresholds = gsonNullable(importedSettings.negativeRankThresholds) ?: emptyList(),
            negativeRankColors = gsonNullable(importedSettings.negativeRankColors) ?: emptyList(),
            negativeRankDivinationLimits = gsonNullable(importedSettings.negativeRankDivinationLimits) ?: emptyList(),
            rankDivinationLimits = gsonNullable(importedSettings.rankDivinationLimits) ?: emptyList(),
            // 事件默认分数（v3.13）：旧导出缺键时补空列表，读取时再经 withNormalizedEventScores 对齐
            goodDeedDefaultScores = gsonNullable(importedSettings.goodDeedDefaultScores) ?: emptyList(),
            badDeedDefaultScores = gsonNullable(importedSettings.badDeedDefaultScores) ?: emptyList(),
            goodResultDefaultScores = gsonNullable(importedSettings.goodResultDefaultScores) ?: emptyList(),
            multiplierPresets = importedMultipliers,
        ))

        val historyArray = root.getAsJsonArray("history") ?: return false
        historyDao.deleteAll()
        val entries = historyArray.mapNotNull { element ->
            val h = element.asJsonObject ?: return@mapNotNull null
            HistoryEntryEntity(
                id = 0,
                timestamp = h.get("timestamp")?.asLong ?: 0L,
                delta = h.get("delta")?.asFloat ?: 0f,
                event = h.get("event")?.asString ?: "",
                type = h.get("type")?.asString ?: "record",
                totalAfter = h.get("totalAfter")?.asFloat ?: 0f,
            )
        }
        historyDao.insertAll(entries)
        return true
    }

    private suspend fun importCsv(raw: String): Boolean {
        val lines = raw.trim().lines()
        if (lines.size < 2) return false

        val entries = mutableListOf<HistoryEntryEntity>()
        var maxId = 0L

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            // Simple CSV parse (no full RFC 4180, but handles quoted fields)
            val cols = parseCsvLine(line)
            if (cols.size < 5) continue

            val id = cols[0].toLongOrNull() ?: i.toLong()
            val timestamp = cols[1].toLongOrNull() ?: 0L
            val delta = cols[2].toFloatOrNull() ?: 0f
            val event = cols[3].trim('"')
            val type = cols[4].trim()
            val totalAfter = cols.getOrNull(5)?.toFloatOrNull() ?: 0f

            entries.add(
                HistoryEntryEntity(
                    id = 0,
                    timestamp = timestamp,
                    delta = delta,
                    event = event,
                    type = type,
                    totalAfter = totalAfter,
                )
            )
            if (id > maxId) maxId = id
        }

        if (entries.isEmpty()) return false

        // Sort by timestamp and recalculate totalAfter
        entries.sortBy { it.timestamp }
        var total = 0f
        for (entry in entries) {
            total = roundToOneDecimal(total + entry.delta)
        }

        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        historyDao.deleteAll()
        historyDao.insertAll(entries)
        settingsDao.upsertSettings(settings.copy(totalScore = total))
        return true
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun roundToOneDecimal(value: Float): Float {
        return kotlin.math.round(value * 10f) / 10f
    }

    /** 誓约分数格式化：整数去小数点（3 而非 3.0），小数保留原样（3.5）。 */
    private fun formatPoints(value: Float): String {
        return if (value % 1f == 0f) value.toInt().toString() else value.toString()
    }

    /**
     * gson 反序列化缺键时字段为 null（unsafe 分配实例，绕过构造函数，默认值不生效），
     * 此辅助函数把「非空声明」的字段按运行时实际值转为可空，供兜底使用。
     */
    private fun <T> gsonNullable(value: T): T? = value
}
