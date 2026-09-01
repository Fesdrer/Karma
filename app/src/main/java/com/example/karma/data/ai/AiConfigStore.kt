package com.example.karma.data.ai

import android.content.Context

/**
 * AI 分析（大模型 API）配置存储。
 *
 * 使用 SharedPreferences 独立存储，不进入 Room 草稿体系，
 * 避免数据库迁移风险。修改后立即生效，无需点击「保存设置」。
 */
class AiConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE)

    /** API 基地址（OpenAI 兼容接口，如 https://api.deepseek.com），自动去尾部斜杠。 */
    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL)!!.trim().trimEnd('/')
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

    /** API Key */
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "")!!.trim()
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    /** 模型名（如 deepseek-chat、gpt-4o-mini 等） */
    var model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL)!!.trim()
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    /** 是否已配置完整（地址与 Key 非空即可发起调用） */
    fun isConfigured(): Boolean = baseUrl.isNotEmpty() && apiKey.isNotEmpty()

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        private const val DEFAULT_MODEL = "deepseek-chat"
    }
}
