package com.example.karma.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.launch
import kotlin.math.round

class TimerViewModel(
    private val repository: KarmaRepository,
    val selectedScore: Float,
    val selectedEvent: String,
) : ViewModel() {

    /** 停止计时：计算分数并写入数据库。返回计算出的 delta。 */
    fun onStop(elapsedMs: Long, onComplete: (Float) -> Unit) {
        val totalSeconds = elapsedMs / 1000.0
        val totalMinutes = totalSeconds / 60.0
        // delta = round((minutes / 15 * score) * 2) / 2  即取最近 0.5
        val product = (totalMinutes / 15.0) * selectedScore
        val rounded = round(product * 2.0) / 2.0
        val delta = rounded.toFloat()

        viewModelScope.launch {
            repository.addHistoryEntry(delta, selectedEvent, "record")
            onComplete(delta)
        }
    }

    class Factory(
        private val repository: KarmaRepository,
        private val score: Float,
        private val event: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimerViewModel(repository, score, event) as T
        }
    }
}
