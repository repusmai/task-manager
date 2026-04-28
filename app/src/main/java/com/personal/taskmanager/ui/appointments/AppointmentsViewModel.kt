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

data class AppointmentsUiState(
    val appointments: List<Appointment> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now()
)

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val repository: TaskRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<AppointmentsUiState> = _selectedDate
        .flatMapLatest { date ->
            repository.getAppointmentsForDate(date).map { appointments ->
                AppointmentsUiState(appointments = appointments, selectedDate = date)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppointmentsUiState())

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    fun addAppointment(appointment: Appointment) = viewModelScope.launch {
        val id = repository.addAppointment(appointment)
        scheduleAppointmentReminder(context, appointment.copy(id = id))
    }

    fun updateAppointment(appointment: Appointment) = viewModelScope.launch {
        repository.updateAppointment(appointment)
        cancelReminder(context, appointment.id + 100000L) // offset to avoid task ID clash
        scheduleAppointmentReminder(context, appointment)
    }

    fun deleteAppointment(appointment: Appointment) = viewModelScope.launch {
        cancelReminder(context, appointment.id + 100000L)
        repository.deleteAppointment(appointment)
    }
}
