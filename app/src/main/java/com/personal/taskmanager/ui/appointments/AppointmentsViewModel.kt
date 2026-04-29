package com.personal.taskmanager.ui.appointments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.taskmanager.data.model.Appointment
import com.personal.taskmanager.data.repository.TaskRepository
import com.personal.taskmanager.notifications.scheduleAppointmentReminder
import com.personal.taskmanager.notifications.cancelReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class AppointmentSortOrder { DATE_ASC, DATE_DESC, TITLE_AZ, DURATION }

data class AppointmentsUiState(
    val appointments: List<Appointment> = emptyList(),
    val allAppointments: List<Appointment> = emptyList(),
    val selectedDates: Set<LocalDate> = emptySet(),
    val sortOrder: AppointmentSortOrder = AppointmentSortOrder.DATE_ASC,
    val latestAppointmentDate: LocalDate? = null
)

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val repository: TaskRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    private val _sortOrder = MutableStateFlow(AppointmentSortOrder.DATE_ASC)

    val uiState: StateFlow<AppointmentsUiState> = combine(
        repository.getAllAppointments(),
        _selectedDates,
        _sortOrder
    ) { allAppointments, dates, sort ->
        val filtered = if (dates.isEmpty()) allAppointments
                       else allAppointments.filter { appt ->
                           dates.any { date -> appt.startDate <= date && appt.endDate >= date }
                       }
        val sorted = when (sort) {
            AppointmentSortOrder.DATE_ASC -> filtered.sortedBy { it.startDate }
            AppointmentSortOrder.DATE_DESC -> filtered.sortedByDescending { it.startDate }
            AppointmentSortOrder.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
            AppointmentSortOrder.DURATION -> filtered.sortedByDescending {
                it.endDate.toEpochDay() - it.startDate.toEpochDay()
            }
        }
        AppointmentsUiState(
            appointments = sorted, allAppointments = allAppointments,
            selectedDates = dates, sortOrder = sort,
            latestAppointmentDate = allAppointments.maxByOrNull { it.startDate }?.startDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppointmentsUiState())

    fun toggleDate(date: LocalDate) {
        _selectedDates.value = _selectedDates.value.let {
            if (date in it) it - date else it + date
        }
    }
    fun clearDateFilter() { _selectedDates.value = emptySet() }
    fun setSortOrder(order: AppointmentSortOrder) { _sortOrder.value = order }

    fun addAppointment(appointment: Appointment) = viewModelScope.launch {
        val id = repository.addAppointment(appointment)
        scheduleAppointmentReminder(context, appointment.copy(id = id))
    }
    fun updateAppointment(appointment: Appointment) = viewModelScope.launch {
        repository.updateAppointment(appointment)
        cancelReminder(context, appointment.id + 100000L)
        scheduleAppointmentReminder(context, appointment)
    }
    fun deleteAppointment(appointment: Appointment) = viewModelScope.launch {
        cancelReminder(context, appointment.id + 100000L)
        repository.deleteAppointment(appointment)
    }
}
