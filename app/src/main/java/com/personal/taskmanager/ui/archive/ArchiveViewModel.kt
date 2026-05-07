package com.personal.taskmanager.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.taskmanager.data.model.Appointment
import com.personal.taskmanager.data.model.Task
import com.personal.taskmanager.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val tasks: List<Task> = emptyList(),
    val appointments: List<Appointment> = emptyList(),
    val searchQuery: String = ""
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ArchiveUiState> = combine(
        repository.getArchivedTasks(),
        repository.getArchivedAppointments(),
        _searchQuery
    ) { tasks, appointments, query ->
        val q = query.trim().lowercase()
        ArchiveUiState(
            tasks = if (q.isEmpty()) tasks else tasks.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) },
            appointments = if (q.isEmpty()) appointments else appointments.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) },
            searchQuery = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveUiState())

    fun setSearch(query: String) { _searchQuery.value = query }

    fun restoreTask(task: Task) = viewModelScope.launch {
        repository.restoreTask(task.id)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun restoreAppointment(appointment: Appointment) = viewModelScope.launch {
        repository.restoreAppointment(appointment.id)
    }

    fun deleteAppointment(appointment: Appointment) = viewModelScope.launch {
        repository.deleteAppointment(appointment)
    }

    fun clearAllArchived() = viewModelScope.launch {
        uiState.value.tasks.forEach { repository.deleteTask(it) }
        uiState.value.appointments.forEach { repository.deleteAppointment(it) }
    }
}
