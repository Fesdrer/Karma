package com.example.karma.di

import android.content.Context
import com.example.karma.data.local.KarmaDatabase
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 桌面组件点击产生的待处理请求（v4.3）。
 * @param name 被点击的每日必做事件名
 * @param nonce 请求序号：用于区分同名事件的多次点击，消费时按 nonce 校验，避免误清后续请求
 */
data class WidgetDeedRequest(val name: String, val nonce: Long)

class AppContainer(context: Context) {

    private val database: KarmaDatabase = KarmaDatabase.getInstance(context)

    private val historyEntryDao = database.historyEntryDao()
    private val karmaSettingsDao = database.karmaSettingsDao()

    val repository: KarmaRepository = KarmaRepository(
        historyDao = historyEntryDao,
        settingsDao = karmaSettingsDao,
    )

    // ===== 桌面组件请求中转（v4.3）=====
    // MainActivity 收到组件点击后写入，MainScreen 在首页组合时消费。
    // 放在容器里是因为 ViewModel 是导航级的：用户停在设置页时 MainScreen 未组合，
    // 请求需先暂存，待导航回首页再处理，避免点击丢失。
    private val _widgetDeedRequest = MutableStateFlow<WidgetDeedRequest?>(null)
    val widgetDeedRequest: StateFlow<WidgetDeedRequest?> = _widgetDeedRequest.asStateFlow()

    /** 桌面组件点击某行圆圈（App 冷启动 / 已开着都走这里）。 */
    fun requestWidgetDeedCompletion(name: String) {
        _widgetDeedRequest.value = WidgetDeedRequest(name, System.nanoTime())
    }

    /** 首页处理完请求后清空（校验 nonce，避免清掉期间新产生的请求）。 */
    fun consumeWidgetDeedRequest(nonce: Long) {
        if (_widgetDeedRequest.value?.nonce == nonce) {
            _widgetDeedRequest.value = null
        }
    }
}
