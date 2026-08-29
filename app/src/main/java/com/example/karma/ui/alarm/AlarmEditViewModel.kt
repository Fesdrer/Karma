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
 * 闹钟新建/编辑页 ViewModel：
 * - draft 为编辑中的草稿（null = 尚未加载完成，UI 不渲染）
 * - 保存：upsert + 重新调度（开→schedule，关→cancel）
 * - 删除：取消调度 + 删除
 */
class AlarmEditViewModel(
    private val repository: KarmaRepository,
    private val application: Application,
) : ViewModel() {

    private val _draft = MutableStateFlow<AlarmEntity?>(null)
    val draft: StateFlow<AlarmEntity?> = _draft.asStateFlow()

    /** 加载编辑对象；alarmId <= 0 表示新建（默认当前时间，照手机闹钟）。 */
    fun load(alarmId: Long) {
        viewModelScope.launch {
            _draft.value = if (alarmId > 0) {
                repository.getAlarmById(alarmId) ?: AlarmEntity()
            } else {
                val now = java.util.Calendar.getInstance()
                AlarmEntity(
                    hour = now.get(java.util.Calendar.HOUR_OF_DAY),
                    minute = now.get(java.util.Calendar.MINUTE),
                )
            }
        }
    }

    fun updateHour(hour: Int) {
        _draft.value = _draft.value?.copy(hour = hour)
    }

    fun updateMinute(minute: Int) {
        _draft.value = _draft.value?.copy(minute = minute)
    }

    fun setRepeatDays(days: List<Int>) {
        _draft.value = _draft.value?.copy(repeatDays = days)
    }

    fun setEventName(name: String) {
        _draft.value = _draft.value?.copy(eventName = name)
    }

    fun setRingtone(uri: String) {
        _draft.value = _draft.value?.copy(ringtoneUri = uri)
    }

    fun setVibrate(vibrate: Boolean) {
        _draft.value = _draft.value?.copy(vibrate = vibrate)
    }

    fun setSnoozeMinutes(minutes: Int) {
        _draft.value = _draft.value?.copy(snoozeMinutes = minutes)
    }

    /** 保存：写库 + 调度（或取消）；完成后回调。 */
    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val d = _draft.value ?: return@launch
            val id = repository.upsertAlarm(d)
            val saved = d.copy(id = id)
            val context = application.applicationContext
            if (saved.enabled) {
                AlarmScheduler.schedule(context, saved)
            } else {
                AlarmScheduler.cancel(context, id)
            }
            onDone()
        }
    }

    /** 删除（仅编辑已有闹钟）；完成后回调。 */
    fun deleteAndDone(onDone: () -> Unit) {
        viewModelScope.launch {
            val d = _draft.value ?: return@launch
            if (d.id > 0) {
                val context = application.applicationContext
                AlarmScheduler.cancel(context, d.id)
                AlarmNotifications.cancel(context, d.id)
                repository.deleteAlarm(d)
            }
            onDone()
        }
    }

    class Factory(
        private val repository: KarmaRepository,
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlarmEditViewModel(repository, application) as T
        }
    }
}
