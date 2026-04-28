package com.personal.taskmanager.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.taskmanager.data.model.*
import com.personal.taskmanager.data.repository.TaskRepository
import com.personal.taskmanager.notifications.cancelReminder
import com.personal.taskmanager.notifications.scheduleReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val filterStatus: TaskStatus? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _filterStatus = MutableStateFlow<TaskStatus?>(null)

    val uiState: StateFlow<TasksUiState> = combine(
        _selectedDate.flatMapLatest { repository.getTasksForDate(it) },
        repository.getAllCategories(),
        _selectedDate,
        _filterStatus
    ) { tasks, categories, date, filter ->
        val filtered = if (filter != null) tasks.filter { it.status == filter } else tasks
        TasksUiState(
            tasks = filtered.sortedWith(
                compareBy<Task> { it.status == TaskStatus.COMPLETED }
                    .thenByDescending { it.priority }
                    .thenBy { it.dueTime }
            ),
            categories = categories,
            selectedDate = date,
            filterStatus = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    fun setFilter(status: TaskStatus?) { _filterStatus.value = status }

    fun addTask(task: Task) = viewModelScope.launch {
        val id = repository.addTask(task)
        scheduleReminder(context, task.copy(id = id))
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
        cancelReminder(context, task.id)
        scheduleReminder(context, task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        cancelReminder(context, task.id)
        repository.deleteTask(task)
    }

    fun markComplete(task: Task) = viewModelScope.launch {
        cancelReminder(context, task.id)
        repository.markComplete(task.id)
        if (task.recurrenceType != RecurrenceType.NONE) {
            val nextDate = when (task.recurrenceType) {
                RecurrenceType.DAILY -> task.dueDate?.plusDays(1)
                RecurrenceType.WEEKLY -> task.dueDate?.plusWeeks(1)
                RecurrenceType.MONTHLY -> task.dueDate?.plusMonths(1)
                else -> null
            }
            nextDate?.let {
                val newTask = task.copy(id = 0, dueDate = it, status = TaskStatus.PENDING, completedAt = null)
                val newId = repository.addTask(newTask)
                scheduleReminder(context, newTask.copy(id = newId))
            }
        }
    }

    fun addCategory(name: String, colorHex: String) = viewModelScope.launch {
        repository.addCategory(Category(name = name, colorHex = colorHex))
    }
}
