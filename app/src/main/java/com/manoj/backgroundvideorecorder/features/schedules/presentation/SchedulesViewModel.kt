package com.manoj.backgroundvideorecorder.features.schedules.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule
import com.manoj.backgroundvideorecorder.features.schedules.domain.usecase.AddScheduleUseCase
import com.manoj.backgroundvideorecorder.features.schedules.domain.usecase.DeleteScheduleUseCase
import com.manoj.backgroundvideorecorder.features.schedules.domain.usecase.GetSchedulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val getSchedulesUseCase: GetSchedulesUseCase,
    private val addScheduleUseCase: AddScheduleUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SchedulesState())
    val state: StateFlow<SchedulesState> = _state.asStateFlow()

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            getSchedulesUseCase()
                .onStart { _state.update { it.copy(isLoading = true) } }
                .catch { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
                .collect { list ->
                    _state.update { it.copy(isLoading = false, schedules = list, errorMessage = null) }
                }
        }
    }

    fun addSchedule(timeMillis: Long, durationSeconds: Int, isRecurring: Boolean) {
        viewModelScope.launch {
            val newSchedule = Schedule(
                scheduledTimeMillis = timeMillis,
                durationSeconds = durationSeconds,
                isRecurring = isRecurring
            )
            addScheduleUseCase(newSchedule)
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            deleteScheduleUseCase(schedule)
        }
    }
}
