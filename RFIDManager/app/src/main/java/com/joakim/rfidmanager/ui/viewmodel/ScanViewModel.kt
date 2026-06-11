package com.joakim.rfidmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScanViewModel : ViewModel() {

    private val _selectedTagUid = MutableStateFlow<String?>(null)
    val selectedTagUid: StateFlow<String?> = _selectedTagUid.asStateFlow()

    fun selectTag(uid: String?) {
        _selectedTagUid.value = uid
    }
}
