package com.example.karma.di

import android.content.Context
import com.example.karma.data.local.KarmaDatabase
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 桌面组件「加分跨入更高正阶位」时的补弹自证请求（v4.3）。
 * 组件点击本身已直接写库，此请求只负责让 App 弹出被动自证询问窗。
 * @param nonce 请求序号：用于区分多次请求，消费时按 nonce 校验，避免误清后续请求
 */
data class WidgetProofRequest(val oldLevel: Int, val newLevel: Int, val nonce: Long)

class AppContainer(context: Context) {

    private val database: KarmaDatabase = KarmaDatabase.getInstance(context)

    private val historyEntryDao = database.historyEntryDao()
    private val karmaSettingsDao = database.karmaSettingsDao()

    val repository: KarmaRepository = KarmaRepository(
        historyDao = historyEntryDao,
        settingsDao = karmaSettingsDao,
    )

    // ===== 桌面组件跨阶补弹自证请求中转（v4.3）=====
    // MainActivity 收到组件跨阶带来的 Intent extra 后写入，MainScreen 在首页组合时消费。
    // 放在容器里是因为 ViewModel 是导航级的：用户停在设置页时 MainScreen 未组合，
    // 请求需先暂存，待导航回首页再处理，避免自证询问窗丢失。
    private val _widgetProofRequest = MutableStateFlow<WidgetProofRequest?>(null)
    val widgetProofRequest: StateFlow<WidgetProofRequest?> = _widgetProofRequest.asStateFlow()

    /** 组件点击加分跨阶：登记一次补弹自证请求。 */
    fun requestWidgetProof(oldLevel: Int, newLevel: Int) {
        _widgetProofRequest.value = WidgetProofRequest(oldLevel, newLevel, System.nanoTime())
    }

    /** 首页处理完请求后清空（校验 nonce，避免清掉期间新产生的请求）。 */
    fun consumeWidgetProofRequest(nonce: Long) {
        if (_widgetProofRequest.value?.nonce == nonce) {
            _widgetProofRequest.value = null
        }
    }
}
