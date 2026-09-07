package com.example.karma.ui.prayer

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PrayerUiState(
    val amount: String = "",
    val purpose: String = "",
    val showForm: Boolean = true,
    val isAnimating: Boolean = false,
    val totalScore: Float = 0f,
    val rankLevel: Int = 1,
    val totalRanks: Int = 9,
    val errorMessage: String? = null,
    val showDeityInput: Boolean = false,
    val deity: String = "",
    // ===== 神秘学符号画板（v4.2）：画作仅自赏，不入事件记录、不持久化 =====
    val showSymbolBoard: Boolean = false,               // 开关：绘制神秘符号
    val symbolStrokes: List<List<Offset>> = emptyList(), // 已完成笔画（坐标已归一化 0~1）
    val currentStroke: List<Offset> = emptyList(),       // 正在画的笔画（归一化 0~1）
)

class PrayerViewModel(
    private val repository: KarmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val score = repository.getTotalScoreOnce()
            val settings = repository.settings.first()
            val rank = repository.getRank(score, settings)
            _uiState.value = _uiState.value.copy(
                totalScore = score,
                rankLevel = rank?.level ?: 1,
                totalRanks = settings.rankNames.size.coerceAtLeast(1),
            )
        }
    }

    fun onAmountChanged(text: String) {
        _uiState.value = _uiState.value.copy(amount = text)
    }

    fun onPurposeChanged(text: String) {
        _uiState.value = _uiState.value.copy(purpose = text)
    }

    fun onToggleDeityInput() {
        _uiState.value = _uiState.value.copy(showDeityInput = !_uiState.value.showDeityInput)
    }

    // ===== 神秘学符号画板（v4.2）=====

    /** 切换「绘制神秘符号」开关。 */
    fun toggleSymbolBoard() {
        _uiState.value = _uiState.value.copy(showSymbolBoard = !_uiState.value.showSymbolBoard)
    }

    /** 清空画板全部笔画。 */
    fun clearSymbolStrokes() {
        _uiState.value = _uiState.value.copy(symbolStrokes = emptyList(), currentStroke = emptyList())
    }

    /** 开始一笔：记录起点（p 为画板坐标 / 画板尺寸，归一化 0~1）。 */
    fun startSymbolStroke(p: Offset) {
        _uiState.value = _uiState.value.copy(currentStroke = listOf(p))
    }

    /** 追加当前笔画路径点（归一化坐标），坐标越界时钳制在 0~1。 */
    fun addSymbolPoint(p: Offset) {
        val clamped = Offset(p.x.coerceIn(0f, 1f), p.y.coerceIn(0f, 1f))
        _uiState.value = _uiState.value.copy(
            currentStroke = _uiState.value.currentStroke + clamped,
        )
    }

    /** 抬笔：≥2 点才算一笔，并入已完成笔画列表；不足则丢弃（防误触一个点）。 */
    fun endSymbolStroke() {
        val cur = _uiState.value.currentStroke
        if (cur.size >= 2) {
            _uiState.value = _uiState.value.copy(
                symbolStrokes = _uiState.value.symbolStrokes + listOf(cur),
                currentStroke = emptyList(),
            )
        } else {
            _uiState.value = _uiState.value.copy(currentStroke = emptyList())
        }
    }

    fun onDeityChanged(text: String) {
        _uiState.value = _uiState.value.copy(deity = text)
    }

    fun confirmPrayer() {
        val state = _uiState.value
        val amount = state.amount.toFloatOrNull()
        if (amount == null || amount <= 0f) {
            _uiState.value = state.copy(errorMessage = "请输入正数的扣减分数")
            return
        }
        if (state.purpose.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入祈福目的")
            return
        }
        if (state.totalScore < 30f) {
            _uiState.value = state.copy(errorMessage = "业力值不足，无法祈福")
            return
        }
        if (state.totalScore - amount <= 0f) {
            _uiState.value = state.copy(errorMessage = "扣减后业力值将归零或为负，请减少扣减分数")
            return
        }

        // Record prayer and start animation
        viewModelScope.launch {
            val deitySuffix = if (state.deity.isNotBlank()) "\n神明：\n${state.deity}" else ""
            repository.addHistoryEntry(-amount, "祈福：\n${state.purpose}$deitySuffix", "prayer")
            _uiState.value = _uiState.value.copy(
                showForm = false,
                isAnimating = true,
            )
        }
    }

    fun cancelPrayer() {
        // Just return - handled by navigation pop
    }

    fun onAnimationComplete() {
        _uiState.value = _uiState.value.copy(
            showForm = true,
            isAnimating = false,
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrayerViewModel(repository) as T
        }
    }
}
