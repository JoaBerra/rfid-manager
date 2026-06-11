package com.joakim.rfidmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joakim.rfidmanager.data.mqtt.MqttSender
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.data.settings.AppSettings
import com.joakim.rfidmanager.domain.model.PersistedReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReadingsViewModel(
    private val repository: PersistedReadingRepository,
    private val settings: AppSettings
) : ViewModel() {

    companion object {
        private val RegexSpecialChars = setOf('\\', '.', '+', '*', '?', '(', ')', '[', ']', '{', '}', '^', '$', '|')
    }

    private val _readings = MutableStateFlow<List<PersistedReading>>(emptyList())
    val readings: StateFlow<List<PersistedReading>> = _readings.asStateFlow()

    private val _filterType = MutableStateFlow<String?>(null)
    val filterType: StateFlow<String?> = _filterType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _displayLimit = MutableStateFlow(settings.pageSize.value)
    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    init {
        viewModelScope.launch {
            repository.load()
            combine(
                repository.getAllReadings(),
                _filterType,
                _searchQuery,
                _displayLimit,
                settings.pageSize
            ) { all, typeFilter, query, limit, _ ->
                var result = all

                if (typeFilter != null) {
                    result = result.filter { it.type.equals(typeFilter, ignoreCase = true) }
                }

                if (query.isNotBlank()) {
                    val q = query.trim().lowercase()
                    val pattern = buildString {
                        var i = 0
                        while (i < q.length) {
                            val c = q[i]
                            when (c) {
                                '*' -> append(".*")
                                '?' -> append(".")
                                else -> {
                                    if (c in RegexSpecialChars) append('\\')
                                    append(c)
                                }
                            }
                            i++
                        }
                    }
                    val regex = try { Regex(pattern) } catch (_: Exception) { null }
                    if (regex != null) {
                        result = result.filter {
                            regex.containsMatchIn(it.uidOrCode.lowercase())
                        }
                    }
                }

                val sorted = result.sortedByDescending { it.timestamp }
                _hasMore.value = sorted.size > limit
                sorted.take(limit)
            }.collect { displayed ->
                _readings.value = displayed
            }
        }
    }

    private fun currentPageSize(): Int = settings.pageSize.value

    fun setFilter(type: String?) {
        _filterType.value = type
        _displayLimit.value = currentPageSize()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _displayLimit.value = currentPageSize()
    }

    fun loadMore() {
        _displayLimit.value += currentPageSize()
    }

    fun onTransmit(reading: PersistedReading) {
        viewModelScope.launch {
            MqttSender.sendReading(reading)
            repository.markAsTransmitted(reading.id)
        }
    }
}
