package com.example.karma.ui.alarm

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.local.entity.AlarmEntity
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 闹钟列表页 ViewModel：列表、开关、删除、启动时重新同步调度（防异常丢失）。
 */
class AlarmViewModel(
    private val repository: KarmaRepository,
    private val application: Application,
) : ViewModel() {

    // 用 MutableStateFlow<T?>(null)，避免 stateIn initialValue 导致首帧闪空列表（CLAUDE.md 规则3）
    private val _alarms = MutableStateFlow<List<AlarmEntity>?>(null)
    val alarms: StateFlow<List<AlarmEntity>?> = _alarms.asStateFlow()

    init {
        viewModelScope.launch {
            repository.alarms.collect { _alarms.value = it }
        }
        resync()
    }

    /** 启动时重新注册所有开启的闹钟（防异常丢失；正常流程每次改动已单独调度）。 */
    fun resync() {
        viewModelScope.launch {
            val context = application.applicationContext
            repository.getEnabledAlarms().forEach { AlarmScheduler.schedule(context, it) }
        }
    }

    /** 开关切换：开 → 调度；关 → 取消调度（条目保留）。 */
    fun toggleEnabled(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(enabled = enabled)
            repository.upsertAlarm(updated)
            val context = application.applicationContext
            if (enabled) {
                AlarmScheduler.schedule(context, updated)
            } else {
                AlarmScheduler.cancel(context, alarm.id)
                AlarmNotifications.cancel(context, alarm.id)
            }
        }
    }

    fun delete(alarm: AlarmEntity) {
        viewModelScope.launch {
            val context = application.applicationContext
            AlarmScheduler.cancel(context, alarm.id)
            AlarmNotifications.cancel(context, alarm.id)
            repository.deleteAlarm(alarm)
        }
    }

    class Factory(
        private val repository: KarmaRepository,
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlarmViewModel(repository, application) as T
        }
    }
}
