package com.personal.taskmanager.ui.tasks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.taskmanager.data.model.*
import com.personal.taskmanager.ui.appointments.AddEditAppointmentSheet
import com.personal.taskmanager.ui.appointments.AppointmentsViewModel
import com.personal.taskmanager.ui.appointments.SimpleDatePickerDialog
import com.personal.taskmanager.ui.appointments.SimpleTimePickerDialog
import com.personal.taskmanager.ui.appointments.AppointmentCard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

enum class AddItemType { TASK, APPOINTMENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateToCalendar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    tasksViewModel: TasksViewModel = hiltViewModel(),
    appointmentsViewModel: AppointmentsViewModel = hiltViewModel()
) {
    val tasksState by tasksViewModel.uiState.collectAsState()
    val appointmentsState by appointmentsViewModel.uiState.collectAsState()

    var showAddSelector by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var showAddAppointment by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Keep both ViewModels in sync on date selection
    LaunchedEffect(tasksState.selectedDate) {
        val date = tasksState.selectedDate
        if (date != null) appointmentsViewModel.selectDate(date)
        else appointmentsViewModel.clearDateFilter()
    }

    val topBarTitle = when (tasksState.selectedDate) {
        null -> "All Items"
        else -> tasksState.selectedDate!!.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Day", fontWeight = FontWeight.Bold)
                        Text(
                            topBarTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Clear date filter button
                    if (tasksState.selectedDate != null) {
                        IconButton(onClick = {
                            tasksViewModel.clearDateFilter()
                        }) {
                            Icon(Icons.Default.FilterAltOff, "Show all")
                        }
                    }
                    IconButton(onClick = onNavigateToRoutines) {
                        Icon(Icons.Default.Loop, "Routines")
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
                onClick = { showAddSelector = true },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Date strip — acts as filter, scrolls to latest appointment
            DateStrip(
                selectedDate = tasksState.selectedDate,
                onDateSelected = tasksViewModel::selectDate,
                taskDates = tasksState.allTasks.mapNotNull { it.dueDate }.toSet(),
                appointmentDates = appointmentsState.allAppointments
                    .flatMap { appt ->
                        generateSequence(appt.startDate) { d ->
                            if (d < appt.endDate) d.plusDays(1) else null
                        }.toList()
                    }.toSet(),
                latestAppointmentDate = appointmentsState.latestAppointmentDate
            )

            // Tab selector
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tasks (${tasksState.tasks.size})") },
                    icon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Appointments (${appointmentsState.appointments.size})") },
                    icon = { Icon(Icons.Default.EventNote, null, modifier = Modifier.size(16.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    FilterRow(current = tasksState.filterStatus, onFilter = tasksViewModel::setFilter)
                    if (tasksState.tasks.isEmpty()) {
                        EmptyState(message = "No tasks", sub = "Tap + to add one")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tasksState.tasks, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    categories = tasksState.categories,
                                    onComplete = { tasksViewModel.markComplete(task) },
                                    onEdit = { taskToEdit = task },
                                    onDelete = { tasksViewModel.deleteTask(task) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (appointmentsState.appointments.isEmpty()) {
                        EmptyState(message = "No appointments", sub = "Tap + to add one")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(appointmentsState.appointments, key = { it.id }) { appt ->
                                AppointmentCard(
                                    appointment = appt,
                                    onEdit = { appointmentToEdit = appt },
                                    onDelete = { appointmentsViewModel.deleteAppointment(appt) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add selector
    if (showAddSelector) {
        ModalBottomSheet(onDismissRequest = { showAddSelector = false }) {
            Column(
                modifier = Modifier.padding(24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("What would you like to add?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).clickable {
                            showAddSelector = false; showAddTask = true
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text("Task", fontWeight = FontWeight.SemiBold)
                            Text("A to-do item with priority and due date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).clickable {
                            showAddSelector = false; showAddAppointment = true
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.EventNote, null, modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                            Text("Appointment", fontWeight = FontWeight.SemiBold)
                            Text("A scheduled event with start and end times",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    if (showAddTask || taskToEdit != null) {
        AddEditTaskSheet(
            task = taskToEdit,
            categories = tasksState.categories,
            initialDate = tasksState.selectedDate ?: LocalDate.now(),
            onDismiss = { showAddTask = false; taskToEdit = null },
            onSave = { task ->
                if (taskToEdit != null) tasksViewModel.updateTask(task)
                else tasksViewModel.addTask(task)
                showAddTask = false; taskToEdit = null
            }
        )
    }

    if (showAddAppointment || appointmentToEdit != null) {
        AddEditAppointmentSheet(
            appointment = appointmentToEdit,
            initialDate = tasksState.selectedDate ?: LocalDate.now(),
            onDismiss = { showAddAppointment = false; appointmentToEdit = null },
            onSave = { appt ->
                if (appointmentToEdit != null) appointmentsViewModel.updateAppointment(appt)
                else appointmentsViewModel.addAppointment(appt)
                showAddAppointment = false; appointmentToEdit = null
            }
        )
    }
}

@Composable
fun DateStrip(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    taskDates: Set<LocalDate> = emptySet(),
    appointmentDates: Set<LocalDate> = emptySet(),
    latestAppointmentDate: LocalDate? = null
) {
    val today = LocalDate.now()

    // Build date range: from 7 days ago to whichever is further: 30 days ahead or latest appointment + 3
    val rangeEnd = maxOf(
        today.plusDays(30),
        latestAppointmentDate?.plusDays(3) ?: today.plusDays(30)
    )
    val rangeStart = today.minusDays(7)
    val dates = generateSequence(rangeStart) { it.plusDays(1) }
        .takeWhile { !it.isAfter(rangeEnd) }
        .toList()

    // Index of today for initial scroll position
    val todayIndex = dates.indexOf(today).coerceAtLeast(0)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to today on first composition
    LaunchedEffect(Unit) {
        listState.scrollToItem(maxOf(0, todayIndex - 2))
    }

    // When a new appointment is added, scroll to show it
    LaunchedEffect(latestAppointmentDate) {
        latestAppointmentDate?.let { latest ->
            val idx = dates.indexOf(latest)
            if (idx >= 0) {
                coroutineScope.launch {
                    listState.animateScrollToItem(maxOf(0, idx - 2))
                }
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val hasTask = date in taskDates
            val hasAppointment = date in appointmentDates

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
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
                // Indicator dots — task dot + appointment dot
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasTask) {
                        Box(
                            Modifier.size(4.dp).clip(CircleShape).background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (hasAppointment) {
                        Box(
                            Modifier.size(4.dp).clip(CircleShape).background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
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
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (isOverdue) { Spacer(Modifier.width(6.dp)); Badge { Text("Overdue") } }
                    task.routineId?.let {
                        Spacer(Modifier.width(6.dp))
                        AssistChip(onClick = {},
                            label = { Text("Routine", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Loop, null, Modifier.size(12.dp)) })
                    }
                }
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    task.dueDate?.let { date ->
                        AssistChip(onClick = {},
                            label = { Text(date.format(DateTimeFormatter.ofPattern("MMM d"))) },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(14.dp)) })
                    }
                    task.dueTime?.let {
                        AssistChip(onClick = {},
                            label = { Text(it.format(DateTimeFormatter.ofPattern("HH:mm"))) },
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
                    Icon(Icons.Default.Delete, "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error)
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
    SuggestionChip(onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = color))
}

@Composable
fun EmptyState(message: String = "Nothing here", sub: String = "") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.titleMedium)
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (task == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Task title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

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

            Text("Priority", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p },
                        label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            Text("Repeat", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceType.values().forEach { r ->
                    FilterChip(selected = recurrence == r, onClick = { recurrence = r },
                        label = { Text(r.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            Text("Reminder", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, 5, 15, 30, 60).forEach { mins ->
                    FilterChip(selected = reminderMinutes == mins, onClick = { reminderMinutes = mins },
                        label = { Text(if (mins == null) "None" else "${mins}m before") })
                }
            }

            if (categories.isNotEmpty()) {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null }, label = { Text("None") })
                    categories.forEach { cat ->
                        FilterChip(selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id }, label = { Text(cat.name) })
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = {
                    if (title.isNotBlank()) {
                        onSave((task ?: Task(title = "")).copy(
                            title = title, description = description, priority = priority,
                            recurrenceType = recurrence, categoryId = selectedCategoryId,
                            reminderMinutesBefore = reminderMinutes, dueDate = dueDate, dueTime = dueTime
                        ))
                    }
                }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }

    if (showDatePicker) {
        SimpleDatePickerDialog(initial = dueDate, onDismiss = { showDatePicker = false },
            onConfirm = { dueDate = it; showDatePicker = false })
    }
    if (showTimePicker) {
        SimpleTimePickerDialog(initial = dueTime, onDismiss = { showTimePicker = false },
            onConfirm = { dueTime = it; showTimePicker = false },
            onClear = { dueTime = null; showTimePicker = false })
    }
}
