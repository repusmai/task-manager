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

enum class SortOrder { DATE_ASC, DATE_DESC, PRIORITY_HIGH, PRIORITY_LOW, TITLE_AZ }

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedDates: Set<LocalDate> = emptySet(),
    val filterStatuses: Set<TaskStatus> = emptySet(),
    val sortOrder: SortOrder = SortOrder.DATE_ASC,
    val isLoading: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    private val _filterStatuses = MutableStateFlow<Set<TaskStatus>>(emptySet())
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ASC)

    // Combine in two steps to stay within the 5-flow typed combine limit
    private val _filterState = combine(
        _selectedDates,
        _filterStatuses,
        _sortOrder
    ) { dates, statuses, sort ->
        Triple(dates, statuses, sort)
    }

    val uiState: StateFlow<TasksUiState> = combine(
        repository.getAllTasks(),
        repository.getAllCategories(),
        _filterState
    ) { allTasks, categories, (dates, statuses, sort) ->
        val dateFiltered = if (dates.isEmpty()) allTasks
                           else allTasks.filter { it.dueDate in dates }
        val statusFiltered = if (statuses.isEmpty()) dateFiltered
                             else dateFiltered.filter { it.status in statuses }
        val sorted = when (sort) {
            SortOrder.DATE_ASC -> statusFiltered.sortedWith(
                compareBy<Task> { it.status == TaskStatus.COMPLETED }.thenBy { it.dueDate }.thenBy { it.dueTime })
            SortOrder.DATE_DESC -> statusFiltered.sortedWith(
                compareBy<Task> { it.status == TaskStatus.COMPLETED }.thenByDescending { it.dueDate }.thenByDescending { it.dueTime })
            SortOrder.PRIORITY_HIGH -> statusFiltered.sortedWith(
                compareBy<Task> { it.status == TaskStatus.COMPLETED }.thenByDescending { it.priority })
            SortOrder.PRIORITY_LOW -> statusFiltered.sortedWith(
                compareBy<Task> { it.status == TaskStatus.COMPLETED }.thenBy { it.priority })
            SortOrder.TITLE_AZ -> statusFiltered.sortedWith(
                compareBy<Task> { it.status == TaskStatus.COMPLETED }.thenBy { it.title.lowercase() })
        }
        TasksUiState(
            tasks = sorted,
            allTasks = allTasks,
            categories = categories,
            selectedDates = dates,
            filterStatuses = statuses,
            sortOrder = sort
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun toggleDate(date: LocalDate) {
        _selectedDates.value = _selectedDates.value.let {
            if (date in it) it - date else it + date
        }
    }
    fun clearDateFilter() { _selectedDates.value = emptySet() }

    fun toggleStatus(status: TaskStatus) {
        _filterStatuses.value = _filterStatuses.value.let {
            if (status in it) it - status else it + status
        }
    }
    fun clearStatusFilter() { _filterStatuses.value = emptySet() }

    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }

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
