package com.example.karma.data.ai

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI 分析客户端：调用 OpenAI 兼容的 /chat/completions 接口。
 *
 * - 使用 HttpURLConnection（系统内置），不引入新依赖；
 * - 使用 Gson 构造请求与解析响应（项目已有 Gson 依赖）；
 * - 网络操作在 Dispatchers.IO 执行。
 *
 * 失败时抛出带中文提示的异常，由 UI 层展示。
 */
object AiAnalysisClient {

    private val gson = Gson()

    private class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.7,
    )

    private class ChatMessage(val role: String, val content: String)

    private class ChatResponse(val choices: List<ChatChoice>?)

    private class ChatChoice(val message: ChatMessage?)

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 90_000

    /**
     * 发起一次占卜 AI 分析。
     *
     * @param divinationType 占卜类型（"大衍筮法"/"小六壬"）
     * @param topic 求占事项
     * @param resultSummary 占卜结果的文本摘要（卦象/宫位等）
     * @return 模型返回的分析文本（已 trim）
     * @throws IllegalArgumentException 配置不完整
     * @throws IOException 网络错误 / HTTP 错误 / 返回格式错误（消息为中文）
     */
    suspend fun analyze(
        context: Context,
        divinationType: String,
        topic: String,
        resultSummary: String,
    ): String = withContext(Dispatchers.IO) {
        val store = AiConfigStore(context.applicationContext)
        if (!store.isConfigured()) {
            throw IllegalArgumentException("尚未配置 AI 分析，请先在「设置 → AI 分析」填写 API 地址与 Key")
        }

        val systemPrompt = buildString {
            append("你是精通《周易》大衍筮法与《小六壬》的传统占卜解卦大师。")
            append("请结合用户求占的事项与占卜结果，用简明、实用、有温度的中文给出分析，不要堆砌术语，直接针对用户的事项给出针对性解读。")
            append("输出格式：1）当前状况解读；2）吉凶趋势判断；3）具体可行的行动建议；4）注意事项与心态提醒。")
        }
        val userPrompt = buildString {
            append("【占卜类型】$divinationType\n")
            append("【求占事项】${topic.ifBlank { "（未填写）" }}\n\n")
            append("【占卜结果】\n$resultSummary\n")
        }

        val url = URL("${store.baseUrl}/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${store.apiKey}")
        }

        val request = ChatRequest(
            model = store.model.ifBlank { "deepseek-chat" },
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt),
            ),
        )

        try {
            conn.outputStream.use { os ->
                os.write(gson.toJson(request).toByteArray(Charsets.UTF_8))
            }
        } catch (e: IOException) {
            throw IOException("无法连接 AI 服务，请检查网络与 API 地址", e)
        }

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val errBody = try {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                } catch (_: IOException) {
                    ""
                }
                val reason = errBody.take(300)
                throw IOException("AI 服务返回错误（HTTP $code）${if (reason.isNotBlank()) "：$reason" else ""}")
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parsed = try {
                gson.fromJson(body, ChatResponse::class.java)
            } catch (e: Exception) {
                throw IOException("AI 返回内容解析失败", e)
            }
            val content = parsed?.choices?.firstOrNull()?.message?.content
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            if (content == null) {
                throw IOException("AI 服务返回了空内容，请检查模型名是否正确")
            }
            content
        } catch (e: IOException) {
            throw e
        } finally {
            conn.disconnect()
        }
    }
}
