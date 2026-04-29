package com.personal.taskmanager.ui.tasks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.taskmanager.data.model.*
import com.personal.taskmanager.ui.appointments.AddEditAppointmentSheet
import com.personal.taskmanager.ui.appointments.AppointmentSortOrder
import com.personal.taskmanager.ui.appointments.AppointmentsViewModel
import com.personal.taskmanager.ui.appointments.AppointmentCard
import com.personal.taskmanager.ui.appointments.SimpleDatePickerDialog
import com.personal.taskmanager.ui.appointments.SimpleTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

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
    var showTaskSortSheet by remember { mutableStateOf(false) }
    var showApptSortSheet by remember { mutableStateOf(false) }

    LaunchedEffect(tasksState.selectedDates) {
        // Sync date selection to appointments
        val dates = tasksState.selectedDates
        if (dates.isEmpty()) appointmentsViewModel.clearDateFilter()
        else dates.forEach { appointmentsViewModel.toggleDate(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Day", fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                tasksState.selectedDates.isEmpty() -> "All Items"
                                tasksState.selectedDates.size == 1 -> tasksState.selectedDates.first().format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                                else -> "${tasksState.selectedDates.size} dates selected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (tasksState.selectedDates.isNotEmpty() || tasksState.filterStatuses.isNotEmpty()) {
                        IconButton(onClick = { tasksViewModel.clearDateFilter(); tasksViewModel.clearStatusFilter() }) {
                            Icon(Icons.Default.FilterAltOff, "Show all")
                        }
                    }
                    IconButton(onClick = onNavigateToRoutines) { Icon(Icons.Default.Loop, "Routines") }
                    IconButton(onClick = onNavigateToCalendar) { Icon(Icons.Default.CalendarMonth, "Calendar") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") }
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
            DateStrip(
                selectedDates = tasksState.selectedDates,
                onDateToggled = tasksViewModel::toggleDate,
                taskDates = tasksState.allTasks.mapNotNull { it.dueDate }.toSet(),
                appointmentDates = appointmentsState.allAppointments.flatMap { appt ->
                    generateSequence(appt.startDate) { d ->
                        if (d < appt.endDate) d.plusDays(1) else null
                    }.toList()
                }.toSet(),
                latestAppointmentDate = appointmentsState.latestAppointmentDate
            )

            // Tab row with filter/sort controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabRow(selectedTabIndex = selectedTab, modifier = Modifier.weight(1f)) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("Tasks (${tasksState.tasks.size})") },
                        icon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp)) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("Appts (${appointmentsState.appointments.size})") },
                        icon = { Icon(Icons.Default.EventNote, null, Modifier.size(16.dp)) })
                }
                IconButton(onClick = {
                    if (selectedTab == 0) showTaskSortSheet = true
                    else showApptSortSheet = true
                }) {
                    Icon(Icons.Default.Sort, "Sort")
                }
            }

            when (selectedTab) {
                0 -> {
                    // Status filter chips
                    StatusFilterRow(selected = tasksState.filterStatuses, onToggle = tasksViewModel::toggleStatus, onClear = tasksViewModel::clearStatusFilter)
                    // Active sort indicator
                    if (tasksState.sortOrder != SortOrder.DATE_ASC) {
                        SortIndicator(
                            label = sortOrderLabel(tasksState.sortOrder),
                            onClear = { tasksViewModel.setSortOrder(SortOrder.DATE_ASC) }
                        )
                    }
                    if (tasksState.tasks.isEmpty()) {
                        EmptyState("No tasks", "Tap + to add one")
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
                    // Active sort indicator for appointments
                    if (appointmentsState.sortOrder != AppointmentSortOrder.DATE_ASC) {
                        SortIndicator(
                            label = apptSortOrderLabel(appointmentsState.sortOrder),
                            onClear = { appointmentsViewModel.setSortOrder(AppointmentSortOrder.DATE_ASC) }
                        )
                    }
                    if (appointmentsState.appointments.isEmpty()) {
                        EmptyState("No appointments", "Tap + to add one")
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
                Text("What would you like to add?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f).clickable { showAddSelector = false; showAddTask = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Task", fontWeight = FontWeight.SemiBold)
                            Text("A to-do item with priority and due date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                        }
                    }
                    Card(modifier = Modifier.weight(1f).clickable { showAddSelector = false; showAddAppointment = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EventNote, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
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

    // Task sort sheet
    if (showTaskSortSheet) {
        ModalBottomSheet(onDismissRequest = { showTaskSortSheet = false }) {
            SortSheet(
                title = "Sort Tasks",
                options = listOf(
                    SortOrder.DATE_ASC to "Date (Earliest first)",
                    SortOrder.DATE_DESC to "Date (Latest first)",
                    SortOrder.PRIORITY_HIGH to "Priority (Highest first)",
                    SortOrder.PRIORITY_LOW to "Priority (Lowest first)",
                    SortOrder.TITLE_AZ to "Title (A-Z)"
                ),
                current = tasksState.sortOrder,
                onSelect = { tasksViewModel.setSortOrder(it); showTaskSortSheet = false }
            )
        }
    }

    // Appointment sort sheet
    if (showApptSortSheet) {
        ModalBottomSheet(onDismissRequest = { showApptSortSheet = false }) {
            SortSheet(
                title = "Sort Appointments",
                options = listOf(
                    AppointmentSortOrder.DATE_ASC to "Date (Earliest first)",
                    AppointmentSortOrder.DATE_DESC to "Date (Latest first)",
                    AppointmentSortOrder.TITLE_AZ to "Title (A-Z)",
                    AppointmentSortOrder.DURATION to "Duration (Longest first)"
                ),
                current = appointmentsState.sortOrder,
                onSelect = { appointmentsViewModel.setSortOrder(it); showApptSortSheet = false }
            )
        }
    }

    if (showAddTask || taskToEdit != null) {
        AddEditTaskSheet(
            task = taskToEdit,
            categories = tasksState.categories,
            initialDate = tasksState.selectedDates.minOrNull() ?: LocalDate.now(),
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
            initialDate = tasksState.selectedDates.minOrNull() ?: LocalDate.now(),
            onDismiss = { showAddAppointment = false; appointmentToEdit = null },
            onSave = { appt ->
                if (appointmentToEdit != null) appointmentsViewModel.updateAppointment(appt)
                else appointmentsViewModel.addAppointment(appt)
                showAddAppointment = false; appointmentToEdit = null
            }
        )
    }
}

fun sortOrderLabel(order: SortOrder) = when (order) {
    SortOrder.DATE_ASC -> "Date ↑"
    SortOrder.DATE_DESC -> "Date ↓"
    SortOrder.PRIORITY_HIGH -> "Priority ↓"
    SortOrder.PRIORITY_LOW -> "Priority ↑"
    SortOrder.TITLE_AZ -> "Title A-Z"
}

fun apptSortOrderLabel(order: AppointmentSortOrder) = when (order) {
    AppointmentSortOrder.DATE_ASC -> "Date ↑"
    AppointmentSortOrder.DATE_DESC -> "Date ↓"
    AppointmentSortOrder.TITLE_AZ -> "Title A-Z"
    AppointmentSortOrder.DURATION -> "Duration ↓"
}

@Composable
fun <T> SortSheet(
    title: String,
    options: List<Pair<T, String>>,
    current: T,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        options.forEach { (order, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (order == current) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelect(order) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                if (order == current) {
                    Icon(Icons.Default.Check, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SortIndicator(label: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Sort, null, modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text("Sorted by: $label", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClear, contentPadding = PaddingValues(4.dp)) {
            Text("Reset", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DateStrip(
    selectedDates: Set<LocalDate>,
    onDateToggled: (LocalDate) -> Unit,
    taskDates: Set<LocalDate> = emptySet(),
    appointmentDates: Set<LocalDate> = emptySet(),
    latestAppointmentDate: LocalDate? = null
) {
    val today = LocalDate.now()
    val rangeEnd = maxOf(today.plusDays(30), latestAppointmentDate?.plusDays(3) ?: today.plusDays(30))
    val rangeStart = today.minusDays(7)
    val dates = generateSequence(rangeStart) { it.plusDays(1) }.takeWhile { !it.isAfter(rangeEnd) }.toList()
    val todayIndex = dates.indexOf(today).coerceAtLeast(0)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { listState.scrollToItem(maxOf(0, todayIndex - 2)) }
    LaunchedEffect(latestAppointmentDate) {
        latestAppointmentDate?.let { latest ->
            val idx = dates.indexOf(latest)
            if (idx >= 0) coroutineScope.launch { listState.animateScrollToItem(maxOf(0, idx - 2)) }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dates) { date ->
            val isSelected = date in selectedDates
            val isToday = date == today
            val hasTask = date in taskDates
            val hasAppointment = date in appointmentDates
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    })
                    .clickable { onDateToggled(date) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(date.format(DateTimeFormatter.ofPattern("EEE")),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasTask) Box(Modifier.size(4.dp).clip(CircleShape).background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary))
                    if (hasAppointment) Box(Modifier.size(4.dp).clip(CircleShape).background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary))
                }
            }
        }
    }
}

@Composable
fun StatusFilterRow(
    selected: Set<TaskStatus>,
    onToggle: (TaskStatus) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip clears all filters
        FilterChip(
            selected = selected.isEmpty(),
            onClick = onClear,
            label = { Text("All") }
        )
        TaskStatus.values().forEach { status ->
            FilterChip(
                selected = status in selected,
                onClick = { onToggle(status) },
                label = { Text(when(status) {
                    TaskStatus.PENDING -> "Pending"
                    TaskStatus.COMPLETED -> "Done"
                    TaskStatus.OVERDUE -> "Overdue"
                }) }
            )
        }
    }
}

@Composable
fun TaskCard(task: Task, categories: List<Category>, onComplete: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                    Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    if (isOverdue) { Spacer(Modifier.width(6.dp)); Badge { Text("Overdue") } }
                    task.routineId?.let {
                        Spacer(Modifier.width(6.dp))
                        AssistChip(onClick = {}, label = { Text("Routine", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.Loop, null, Modifier.size(12.dp)) })
                    }
                }
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    task.dueDate?.let { date ->
                        AssistChip(onClick = {}, label = { Text(date.format(DateTimeFormatter.ofPattern("MMM d"))) },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(14.dp)) })
                    }
                    task.dueTime?.let {
                        AssistChip(onClick = {}, label = { Text(it.format(DateTimeFormatter.ofPattern("HH:mm"))) },
                            leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(14.dp)) })
                    }
                    category?.let {
                        AssistChip(onClick = {}, label = { Text(it.name) }, leadingIcon = {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(it.colorHex))))
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
fun EmptyState(message: String = "Nothing here", sub: String = "") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.titleMedium)
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var recurrenceDays by remember {
        mutableStateOf(task?.recurrenceDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet())
    }
    var recurrenceSpecificDates by remember {
        mutableStateOf(
            if (task?.recurrenceType == RecurrenceType.SPECIFIC_DATES)
                task.recurrenceDays.split(",").mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }.toMutableList()
            else mutableListOf()
        )
    }
    var recurrenceTime by remember { mutableStateOf(task?.recurrenceTime) }
    var selectedCategoryId by remember { mutableStateOf(task?.categoryId) }
    var reminderMinutes by remember { mutableStateOf(task?.reminderMinutesBefore) }
    var dueDate by remember { mutableStateOf(task?.dueDate ?: initialDate) }
    var dueTime by remember { mutableStateOf(task?.dueTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRecurrenceTimePicker by remember { mutableStateOf(false) }
    var showSpecificDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dayLabels = listOf(1 to "Mo", 2 to "Tu", 3 to "We", 4 to "Th", 5 to "Fr", 6 to "Sa", 7 to "Su")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()),
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

            // Recurrence
            Text("Repeat", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceType.values().forEach { r ->
                    FilterChip(selected = recurrence == r, onClick = { recurrence = r },
                        label = { Text(when(r) {
                            RecurrenceType.NONE -> "None"
                            RecurrenceType.DAILY -> "Daily"
                            RecurrenceType.WEEKLY -> "Weekly"
                            RecurrenceType.MONTHLY -> "Monthly"
                            RecurrenceType.CUSTOM_DAYS -> "Custom Days"
                            RecurrenceType.SPECIFIC_DATES -> "Specific Dates"
                        }, maxLines = 1, softWrap = false) })
                }
            }

            // Custom days selector
            if (recurrence == RecurrenceType.CUSTOM_DAYS) {
                Text("Repeat on days", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayLabels.forEach { (num, label) ->
                        FilterChip(
                            selected = num in recurrenceDays,
                            onClick = { recurrenceDays = if (num in recurrenceDays) recurrenceDays - num else recurrenceDays + num },
                            label = { Text(label, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                OutlinedButton(onClick = { showRecurrenceTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(recurrenceTime?.format(timeFormatter) ?: "Set time (optional)")
                }
            }

            // Specific dates selector
            if (recurrence == RecurrenceType.SPECIFIC_DATES) {
                Text("Repeat on specific dates", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                recurrenceSpecificDates.forEachIndexed { index, date ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(date.format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy")),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = {
                            recurrenceSpecificDates = recurrenceSpecificDates.toMutableList().also { it.removeAt(index) }
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                OutlinedButton(onClick = { showSpecificDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add date")
                }
                if (recurrenceSpecificDates.isNotEmpty()) {
                    OutlinedButton(onClick = { showRecurrenceTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(recurrenceTime?.format(timeFormatter) ?: "Set time (optional)")
                    }
                }
            }

            Text("Reminder", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, 5, 15, 30, 60).forEach { mins ->
                    FilterChip(selected = reminderMinutes == mins, onClick = { reminderMinutes = mins },
                        label = { Text(if (mins == null) "None" else "${mins}m before") })
                }
            }

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
                Button(onClick = {
                    if (title.isNotBlank()) {
                        val recDays = when (recurrence) {
                            RecurrenceType.CUSTOM_DAYS -> recurrenceDays.sorted().joinToString(",")
                            RecurrenceType.SPECIFIC_DATES -> recurrenceSpecificDates.sorted().joinToString(",")
                            else -> ""
                        }
                        onSave((task ?: Task(title = "")).copy(
                            title = title, description = description, priority = priority,
                            recurrenceType = recurrence, recurrenceDays = recDays,
                            recurrenceTime = if (recurrence == RecurrenceType.NONE) null else recurrenceTime,
                            categoryId = selectedCategoryId, reminderMinutesBefore = reminderMinutes,
                            dueDate = dueDate, dueTime = dueTime
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
    if (showRecurrenceTimePicker) {
        SimpleTimePickerDialog(initial = recurrenceTime, onDismiss = { showRecurrenceTimePicker = false },
            onConfirm = { recurrenceTime = it; showRecurrenceTimePicker = false },
            onClear = { recurrenceTime = null; showRecurrenceTimePicker = false })
    }
    if (showSpecificDatePicker) {
        SimpleDatePickerDialog(initial = LocalDate.now(), onDismiss = { showSpecificDatePicker = false },
            onConfirm = { date ->
                if (!recurrenceSpecificDates.contains(date)) {
                    recurrenceSpecificDates = (recurrenceSpecificDates + date).sortedBy { it }.toMutableList()
                }
                showSpecificDatePicker = false
            })
    }
}
