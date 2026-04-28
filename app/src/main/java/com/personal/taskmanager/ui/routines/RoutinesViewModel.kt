package com.personal.taskmanager.ui.routines

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.taskmanager.data.model.*
import com.personal.taskmanager.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class RoutineWithSteps(val routine: Routine, val steps: List<RoutineStep>)

data class RoutinesUiState(
    val routines: List<RoutineWithSteps> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val repository: TaskRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<RoutinesUiState> = repository.getAllRoutines()
        .flatMapLatest { routines ->
            if (routines.isEmpty()) {
                flowOf(RoutinesUiState(routines = emptyList()))
            } else {
                combine(routines.map { routine ->
                    repository.getStepsForRoutine(routine.id).map { steps ->
                        RoutineWithSteps(routine, steps)
                    }
                }) { array -> RoutinesUiState(routines = array.toList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoutinesUiState(isLoading = true))

    fun saveRoutine(routine: Routine, steps: List<RoutineStep>) = viewModelScope.launch {
        val id = if (routine.id == 0L) repository.addRoutine(routine)
                 else { repository.updateRoutine(routine); routine.id }
        repository.replaceSteps(id, steps)
    }

    fun deleteRoutine(routine: Routine) = viewModelScope.launch {
        repository.deleteRoutine(routine)
    }

    fun toggleActive(routine: Routine) = viewModelScope.launch {
        repository.updateRoutine(routine.copy(isActive = !routine.isActive))
    }

    // Spawn all steps as real tasks for today
    fun startRoutine(routineWithSteps: RoutineWithSteps) = viewModelScope.launch {
        val today = LocalDate.now()
        routineWithSteps.steps.forEach { step ->
            repository.addTask(
                Task(
                    title = step.title,
                    description = step.description,
                    dueDate = today,
                    priority = step.priority,
                    routineId = routineWithSteps.routine.id,
                    status = TaskStatus.PENDING
                )
            )
        }
    }
}
