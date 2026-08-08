package com.example.karma.ui.bet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.local.entity.Bet
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BetViewModel(
    private val repository: KarmaRepository,
) : ViewModel() {
    // CLAUDE.md 规则3：不用 stateIn(initialValue=emptyList) —— 首帧会闪"空列表"，
    // 用 MutableStateFlow<null>，UI 层 null 时不渲染列表区。
    private val _bets = MutableStateFlow<List<Bet>?>(null)
    val bets: StateFlow<List<Bet>?> = _bets.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { _bets.value = it.bets }
        }
    }

    fun addBet(content: String, deadline: String, success: Float, failure: Float) {
        viewModelScope.launch {
            repository.addBet(Bet(content, deadline, success, failure, System.currentTimeMillis()))
        }
    }

    fun resolveBet(bet: Bet, success: Boolean) {
        viewModelScope.launch { repository.resolveBet(bet, success) }
    }

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BetViewModel(repository) as T
        }
    }
}
