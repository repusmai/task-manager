package com.personal.taskmanager.ui.tasks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.taskmanager.data.model.*
import com.personal.taskmanager.ui.appointments.SimpleDatePickerDialog
import com.personal.taskmanager.ui.appointments.SimpleTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateToCalendar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddTask by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Tasks", fontWeight = FontWeight.Bold)
                        Text(
                            state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAppointments) {
                        Icon(Icons.Default.EventNote, "Appointments")
                    }
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Default.CalendarMonth, "Calendar")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTask = true },
                icon = { Icon(Icons.Default.Add, "Add task") },
                text = { Text("New Task") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            DateStrip(selectedDate = state.selectedDate, onDateSelected = viewModel::selectDate)
            FilterRow(current = state.filterStatus, onFilter = viewModel::setFilter)

            if (state.tasks.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            categories = state.categories,
                            onComplete = { viewModel.markComplete(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddTask || taskToEdit != null) {
        AddEditTaskSheet(
            task = taskToEdit,
            categories = state.categories,
            initialDate = state.selectedDate,
            onDismiss = { showAddTask = false; taskToEdit = null },
            onSave = { task ->
                if (taskToEdit != null) viewModel.updateTask(task)
                else viewModel.addTask(task)
                showAddTask = false; taskToEdit = null
            }
        )
    }
}

@Composable
fun DateStrip(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val dates = (-3..10).map { today.plusDays(it.toLong()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dates.forEach { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else if (isToday) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEE")),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FilterRow(current: TaskStatus?, onFilter: (TaskStatus?) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = current == null, onClick = { onFilter(null) }, label = { Text("All") })
        FilterChip(selected = current == TaskStatus.PENDING, onClick = { onFilter(TaskStatus.PENDING) }, label = { Text("Pending") })
        FilterChip(selected = current == TaskStatus.COMPLETED, onClick = { onFilter(TaskStatus.COMPLETED) }, label = { Text("Done") })
        FilterChip(selected = current == TaskStatus.OVERDUE, onClick = { onFilter(TaskStatus.OVERDUE) }, label = { Text("Overdue") })
    }
}

@Composable
fun TaskCard(
    task: Task,
    categories: List<Category>,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val category = categories.find { it.id == task.categoryId }
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isOverdue = task.status == TaskStatus.OVERDUE
    val cardColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        Priority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isCompleted, onCheckedChange = { if (!isCompleted) onComplete() })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (isOverdue) { Spacer(Modifier.width(6.dp)); Badge { Text("Overdue") } }
                }
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    task.dueTime?.let {
                        AssistChip(onClick = {}, label = { Text(it.format(DateTimeFormatter.ofPattern("HH:mm"))) },
                            leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(14.dp)) })
                    }
                    category?.let {
                        AssistChip(onClick = {}, label = { Text(it.name) },
                            leadingIcon = {
                                Box(Modifier.size(8.dp).clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(it.colorHex))))
                            })
                    }
                    PriorityChip(task.priority)
                }
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PriorityChip(priority: Priority) {
    val (label, color) = when (priority) {
        Priority.HIGH -> "High" to MaterialTheme.colorScheme.error
        Priority.MEDIUM -> "Medium" to MaterialTheme.colorScheme.primary
        Priority.LOW -> "Low" to MaterialTheme.colorScheme.tertiary
    }
    SuggestionChip(onClick = {}, label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = color))
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Text("No tasks for this day", style = MaterialTheme.typography.titleMedium)
            Text("Tap + to add one", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskSheet(
    task: Task?,
    categories: List<Category>,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    var recurrence by remember { mutableStateOf(task?.recurrenceType ?: RecurrenceType.NONE) }
    var selectedCategoryId by remember { mutableStateOf(task?.categoryId) }
    var reminderMinutes by remember { mutableStateOf(task?.reminderMinutesBefore) }
    var dueDate by remember { mutableStateOf(task?.dueDate ?: initialDate) }
    var dueTime by remember { mutableStateOf(task?.dueTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (task == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Task title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            // Due date and time
            Text("Due Date & Time", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(dueDate.format(dateFormatter), maxLines = 1)
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(dueTime?.format(timeFormatter) ?: "No time")
                }
            }

            // Priority
            Text("Priority", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p },
                        label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            // Recurrence
            Text("Repeat", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceType.values().forEach { r ->
                    FilterChip(selected = recurrence == r, onClick = { recurrence = r },
                        label = { Text(r.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            // Reminder
            Text("Reminder", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, 5, 15, 30, 60).forEach { mins ->
                    FilterChip(selected = reminderMinutes == mins, onClick = { reminderMinutes = mins },
                        label = { Text(if (mins == null) "None" else "${mins}m before") })
                }
            }

            // Category
            if (categories.isNotEmpty()) {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedCategoryId == null, onClick = { selectedCategoryId = null }, label = { Text("None") })
                    categories.forEach { cat ->
                        FilterChip(selected = selectedCategoryId == cat.id, onClick = { selectedCategoryId = cat.id }, label = { Text(cat.name) })
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave((task ?: Task(title = "")).copy(
                                title = title, description = description, priority = priority,
                                recurrenceType = recurrence, categoryId = selectedCategoryId,
                                reminderMinutesBefore = reminderMinutes, dueDate = dueDate, dueTime = dueTime
                            ))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }

    if (showDatePicker) {
        SimpleDatePickerDialog(
            initial = dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { dueDate = it; showDatePicker = false }
        )
    }
    if (showTimePicker) {
        SimpleTimePickerDialog(
            initial = dueTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { dueTime = it; showTimePicker = false },
            onClear = { dueTime = null; showTimePicker = false }
        )
    }
}
