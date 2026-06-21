package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class WheelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WheelRepository

    init {
        val database = WheelDatabase.getDatabase(application)
        repository = WheelRepository(database.wheelDao())
    }

    val allLists: StateFlow<List<ListWithOption>> = repository.allListsWithFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeList = MutableStateFlow<ListWithOption?>(null)
    val activeList: StateFlow<ListWithOption?> = _activeList.asStateFlow()

    // Animation & spinning state
    private val _isSpinning = MutableStateFlow(false)
    val isSpinning = _isSpinning.asStateFlow()

    private val _spinResult = MutableStateFlow<WheelOption?>(null)
    val spinResult = _spinResult.asStateFlow()

    private val _targetAngle = MutableStateFlow(0f)
    val targetAngle = _targetAngle.asStateFlow()

    init {
        // Prepopulate database if empty and autoset active list
        viewModelScope.launch {
            allLists.collect { lists ->
                if (lists.isEmpty()) {
                    prepopulateDefaults()
                } else if (_activeList.value == null) {
                    // Auto-load first list
                    _activeList.value = lists.first()
                } else {
                    // Update active list if its contents were modified
                    val currentActiveId = _activeList.value?.decisionList?.id
                    val updatedList = lists.find { it.decisionList.id == currentActiveId }
                    if (updatedList != null) {
                        _activeList.value = updatedList
                    }
                }
            }
        }
    }

    fun selectActiveList(list: ListWithOption) {
        if (_isSpinning.value) return // Prevent selector change mid-spin
        _activeList.value = list
        _spinResult.value = null
        _targetAngle.value = 0f
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch {
            repository.deleteListById(listId)
            if (_activeList.value?.decisionList?.id == listId) {
                _activeList.value = null
                _spinResult.value = null
                _targetAngle.value = 0f
            }
        }
    }

    fun createNewList(title: String, options: List<Pair<String, String>>) {
        viewModelScope.launch {
            val listId = repository.insertList(title, options)
            // Load list automatically
            val savedLists = allLists.first { it.isNotEmpty() }
            savedLists.find { it.decisionList.id == listId }?.let {
                selectActiveList(it)
            }
        }
    }

    fun updateList(listId: Long, title: String, options: List<Pair<String, String>>) {
        viewModelScope.launch {
            repository.updateListWithOptions(listId, title, options)
        }
    }

    fun startSpin(currentAngle: Float) {
        val options = _activeList.value?.options ?: return
        if (options.isEmpty() || _isSpinning.value) return

        _isSpinning.value = true
        _spinResult.value = null

        val selectedIndex = Random.nextInt(options.size)
        val selectedOption = options[selectedIndex]

        val sliceAngle = 360f / options.size
        // align middle of physical slice with 270 degrees (Top Point)
        val middleAngle = (selectedIndex + 0.5f) * sliceAngle
        val sliceTargetAngle = (270f - middleAngle + 360f) % 360f

        val currentBase = currentAngle % 360f
        val angleDiff = (sliceTargetAngle - currentBase + 360f) % 360f
        val additionalRotation = 6 * 360f + angleDiff // 6 full spins!
        
        _targetAngle.value = currentAngle + additionalRotation
    }

    fun onSpinAnimationEnd(selectedIndex: Int) {
        _isSpinning.value = false
        val options = _activeList.value?.options ?: return
        if (selectedIndex >= 0 && selectedIndex < options.size) {
            _spinResult.value = options[selectedIndex]
        }
    }

    fun clearResult() {
        _spinResult.value = null
    }

    private suspend fun prepopulateDefaults() {
        val id1 = repository.insertList(
            "🍔 What to Eat?",
            listOf(
                "Pizza" to "#FF5252",
                "Sushi" to "#448AFF",
                "Burgers" to "#FF9100",
                "Salad" to "#00E676",
                "Tacos" to "#FFD700",
                "Pasta" to "#E040FB"
            )
        )
        repository.insertList(
            "❓ Yes or No?",
            listOf(
                "Yes" to "#00E676",
                "No" to "#FF5252",
                "Maybe" to "#FFD700",
                "Spin Again" to "#448AFF"
            )
        )
        repository.insertList(
            "🎬 Movie Night",
            listOf(
                "Action" to "#FF3D00",
                "Comedy" to "#EEFF41",
                "Drama" to "#7C4DFF",
                "Sci-Fi" to "#00E5FF",
                "Horror" to "#212121",
                "Romance" to "#FF4081"
            )
        )
        repository.insertList(
            "🪙 Coin Toss",
            listOf(
                "Heads" to "#FF9100",
                "Tails" to "#00E5FF"
            )
        )
    }
}
