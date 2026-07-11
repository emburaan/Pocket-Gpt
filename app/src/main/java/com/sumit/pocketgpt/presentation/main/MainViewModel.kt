package com.sumit.pocketgpt.presentation.main

import androidx.lifecycle.ViewModel
import com.sumit.pocketgpt.data.inference.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    modelManager: ModelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(statusText = modelManager.status))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
}

data class MainUiState(
    val statusText: String = ""
)