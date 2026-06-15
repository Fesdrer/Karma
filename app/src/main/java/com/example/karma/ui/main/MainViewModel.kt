package com.example.karma.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.model.Rank
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val totalScore: Float = 0f,
    val rank: Rank? = null,
    val scorePresets: List<Float> = emptyList(),
    val eventPresets: List<String> = emptyList(),
    val selectedScore: Float? = null,
    val selectedEvent: String? = null,
    val scoreEditMode: Boolean = false,
    val eventEditMode: Boolean = false,
)

class MainViewModel(
    private val repository: KarmaRepository,
) : ViewModel() {

    private val _selectedScore = MutableStateFlow<Float?>(null)
    private val _selectedEvent = MutableStateFlow<String?>(null)
    private val _customScore = MutableStateFlow<Float?>(null)
    private val _customEvent = MutableStateFlow<String?>(null)
    private val _scoreEditMode = MutableStateFlow(false)
    private val _eventEditMode = MutableStateFlow(false)

    private val _effectiveScore = combine(
        _selectedScore, _customScore
    ) { presetScore, custom -> custom ?: presetScore }

    private val _effectiveEvent = combine(
        _selectedEvent, _customEvent
    ) { presetEvent, custom -> custom ?: presetEvent }

    private val _selectedPair = combine(
        _effectiveScore, _effectiveEvent
    ) { score, event -> Pair(score, event) }

    private val _editModePair = combine(
        _scoreEditMode, _eventEditMode
    ) { sc, ev -> Pair(sc, ev) }

    val uiState: StateFlow<MainUiState> = combine(
        repository.settings,
        _selectedPair,
        _editModePair,
    ) { settings, selection, editMode ->
        MainUiState(
            totalScore = settings.totalScore,
            rank = repository.getRank(settings.totalScore),
            scorePresets = settings.scorePresets,
            eventPresets = settings.eventPresets,
            selectedScore = selection.first,
            selectedEvent = selection.second,
            scoreEditMode = editMode.first,
            eventEditMode = editMode.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    // ---- Actions ----

    fun selectScore(score: Float) {
        _selectedScore.value = score
        _customScore.value = null
    }

    fun selectEvent(event: String) {
        _selectedEvent.value = event
        _customEvent.value = null
    }

    fun onCustomScoreChanged(text: String) {
        val v = text.toFloatOrNull()
        if (v != null) {
            _customScore.value = v
            _selectedScore.value = null
        } else {
            _customScore.value = null
        }
    }

    fun onCustomEventChanged(text: String) {
        val v = text.trim()
        if (v.isNotEmpty()) {
            _customEvent.value = v
            _selectedEvent.value = null
        } else {
            _customEvent.value = null
        }
    }

    fun onConfirm() {
        val score = _customScore.value ?: _selectedScore.value ?: return
        val event = _customEvent.value ?: _selectedEvent.value ?: return

        viewModelScope.launch {
            repository.addHistoryEntry(score, event, "record")
            _selectedScore.value = null
            _selectedEvent.value = null
            _customScore.value = null
            _customEvent.value = null
        }
    }

    fun openScoreEdit() {
        _scoreEditMode.value = true
    }

    fun closeScoreEdit() {
        _scoreEditMode.value = false
    }

    fun openEventEdit() {
        _eventEditMode.value = true
    }

    fun closeEventEdit() {
        _eventEditMode.value = false
    }

    fun saveScorePresets(presets: List<Float>) {
        viewModelScope.launch {
            repository.updateScorePresets(presets)
            closeScoreEdit()
        }
    }

    fun saveEventPresets(presets: List<String>) {
        viewModelScope.launch {
            repository.updateEventPresets(presets)
            closeEventEdit()
        }
    }

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
