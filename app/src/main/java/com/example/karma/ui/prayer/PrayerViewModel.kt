package com.example.karma.ui.prayer

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

        // Record prayer and start animation
        viewModelScope.launch {
            repository.addHistoryEntry(-amount, "祈福：${state.purpose}", "prayer")
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
