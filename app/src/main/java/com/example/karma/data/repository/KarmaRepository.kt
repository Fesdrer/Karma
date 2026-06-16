package com.example.karma.data.repository

import com.example.karma.data.local.dao.HistoryEntryDao
import com.example.karma.data.local.dao.KarmaSettingsDao
import com.example.karma.data.local.entity.HistoryEntryEntity
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.model.Rank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class KarmaRepository(
    private val historyDao: HistoryEntryDao,
    private val settingsDao: KarmaSettingsDao,
) {
    companion object {
        private const val MAX_HISTORY = 2000
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
        it ?: KarmaSettingsEntity()
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
        val todayStr = formatDate(now)

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
        val dateB = formatDate(calB.timeInMillis)

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

        repeat(daysToCatchUp) {
            val rank = getDecayRank(currentScore)
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

        // 更新总分和 lastDecayDate
        settingsDao.upsertSettings(settings.copy(
            totalScore = currentScore,
            lastDecayDate = dateB,
        ))

        return totalDeducted
    }

    /**
     * 根据分数确定阶位（1~9）。
     * 负数归为一阶。
     */
    private fun getDecayRank(score: Float): Int {
        return when {
            score < 0 -> 1
            score < 10 -> 1
            score < 30 -> 2
            score < 60 -> 3
            score < 100 -> 4
            score < 150 -> 5
            score < 210 -> 6
            score < 280 -> 7
            score < 360 -> 8
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

    /**
     * 将时间戳格式化为 "yyyy-MM-dd"。
     */
    private fun formatDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return String.format("%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH))
    }

    // ---- Rank ----

    fun getRank(score: Float): Rank? {
        if (score < 0) return null
        return Rank.RANKS.find { score >= it.min && score < it.max }
    }

    // ---- Import / Export ----

    suspend fun exportJson(): String {
        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        val history = historyDao.getAllEntriesList()
        val data = mapOf(
            "totalScore" to settings.totalScore,
            "history" to history.map { entry ->
                mapOf(
                    "id" to entry.id,
                    "timestamp" to entry.timestamp,
                    "delta" to entry.delta,
                    "event" to entry.event,
                    "type" to entry.type,
                    "totalAfter" to entry.totalAfter,
                )
            }
        )
        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data)
    }

    suspend fun exportCsv(): String {
        val history = historyDao.getAllEntriesList()
        val sb = StringBuilder()
        sb.appendLine("timestamp,delta,event,totalAfter,type")
        for (h in history) {
            val ev = "\"" + h.event.replace("\"", "\"\"") + "\""
            sb.appendLine("${h.timestamp},${h.delta},${ev},${h.totalAfter},${h.type}")
        }
        return sb.toString()
    }

    suspend fun importData(raw: String, format: String): Boolean {
        return try {
            if (format == "json") {
                importJson(raw)
            } else {
                importCsv(raw)
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun importJson(raw: String): Boolean {
        val gson = com.google.gson.Gson()
        val map = gson.fromJson(raw, Map::class.java)
        val totalScore = (map["totalScore"] as? Number)?.toFloat() ?: return false
        val historyRaw = map["history"] as? List<*> ?: return false

        val settings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
        historyDao.deleteAll()
        val entries = historyRaw.mapNotNull { item ->
            val h = item as? Map<*, *> ?: return@mapNotNull null
            HistoryEntryEntity(
                id = 0,
                timestamp = (h["timestamp"] as? Number)?.toLong() ?: 0L,
                delta = (h["delta"] as? Number)?.toFloat() ?: 0f,
                event = (h["event"] as? String) ?: "",
                type = (h["type"] as? String) ?: "record",
                totalAfter = (h["totalAfter"] as? Number)?.toFloat() ?: 0f,
            )
        }
        historyDao.insertAll(entries)
        settingsDao.upsertSettings(settings.copy(totalScore = totalScore))
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
}
