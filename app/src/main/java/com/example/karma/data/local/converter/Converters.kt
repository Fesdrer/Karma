package com.example.karma.data.local.converter

import androidx.room.TypeConverter
import com.example.karma.data.local.entity.Bet
import com.example.karma.data.local.entity.DailyMustDoDeed
import com.example.karma.data.model.Fraction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromFractionList(value: List<Fraction>): String {
        return gson.toJson(value)
    }

    /**
     * 读取乘数列表。兼容两种格式：
     * - 新格式：[{"numerator":1,"denominator":3}, ...]（对象数组）
     * - 旧格式：[0.33333334, 0.75, ...]（Float 数组，v20 时代存储的），自动还原为分数
     */
    @TypeConverter
    fun toFractionList(value: String): List<Fraction> {
        return try {
            val arr = gson.fromJson(value, com.google.gson.JsonArray::class.java) ?: return emptyList()
            arr.mapNotNull { el ->
                when {
                    el.isJsonObject -> {
                        val obj = el.asJsonObject
                        val n = obj.get("numerator")?.asLong ?: return@mapNotNull null
                        val d = obj.get("denominator")?.asLong ?: return@mapNotNull null
                        Fraction.of(n, d)
                    }
                    el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> {
                        Fraction.fromFloat(el.asFloat)
                    }
                    else -> null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFloatList(value: List<Float>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFloatList(value: String): List<Float> {
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromDailyMustDoDeedList(value: List<DailyMustDoDeed>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDailyMustDoDeedList(value: String): List<DailyMustDoDeed> {
        val type = object : TypeToken<List<DailyMustDoDeed>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromBetList(value: List<Bet>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBetList(value: String): List<Bet> {
        val type = object : TypeToken<List<Bet>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
