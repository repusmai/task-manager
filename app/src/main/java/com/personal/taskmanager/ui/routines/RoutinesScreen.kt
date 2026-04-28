package com.personal.taskmanager.ui.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.taskmanager.data.model.*
import com.personal.taskmanager.ui.appointments.SimpleTimePickerDialog
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onNavigateBack: () -> Unit,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<RoutineWithSteps?>(null) }
    var routineToConfirmStart by remember { mutableStateOf<RoutineWithSteps?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("New Routine") }
            )
        }
    ) { padding ->
        if (state.routines.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Loop, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("No routines yet", style = MaterialTheme.typography.titleMedium)
                    Text("Tap + to create one",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.routines, key = { it.routine.id }) { rws ->
                    RoutineCard(
                        routineWithSteps = rws,
                        onStart = { routineToConfirmStart = rws },
                        onEdit = { routineToEdit = rws },
                        onDelete = { viewModel.deleteRoutine(rws.routine) },
                        onToggleActive = { viewModel.toggleActive(rws.routine) }
                    )
                }
            }
        }
    }

    // Confirm start dialog
    routineToConfirmStart?.let { rws ->
        AlertDialog(
            onDismissRequest = { routineToConfirmStart = null },
            title = { Text("Start \"${rws.routine.name}\"?") },
            text = {
                Text("This will add ${rws.steps.size} task${if (rws.steps.size != 1) "s" else ""} to today's task list.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.startRoutine(rws)
                    routineToConfirmStart = null
                }) { Text("Start Routine") }
            },
            dismissButton = {
                TextButton(onClick = { routineToConfirmStart = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddSheet || routineToEdit != null) {
        AddEditRoutineSheet(
            routineWithSteps = routineToEdit,
            onDismiss = { showAddSheet = false; routineToEdit = null },
            onSave = { routine, steps ->
                viewModel.saveRoutine(routine, steps)
                showAddSheet = false; routineToEdit = null
            }
        )
    }
}

@Composable
fun RoutineCard(
    routineWithSteps: RoutineWithSteps,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val routine = routineWithSteps.routine
    val steps = routineWithSteps.steps
    val color = try { Color(android.graphics.Color.parseColor(routine.colorHex)) }
                catch (e: Exception) { MaterialTheme.colorScheme.primary }
    var expanded by remember { mutableStateOf(false) }

    val scheduleDays = routine.scheduledDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    val scheduleText = buildString {
        if (scheduleDays.isNotEmpty()) {
            append(scheduleDays.joinToString(", ") {
                DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            })
            routine.scheduledTime?.let { append(" at ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}") }
        } else {
            append("Manual only")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(12.dp).clip(CircleShape)
                        .background(if (routine.isActive) color else color.copy(alpha = 0.3f))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(routine.name, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (routine.isActive) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(scheduleText, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${steps.size} step${if (steps.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = color)
                }
                // Active toggle
                Switch(checked = routine.isActive, onCheckedChange = { onToggleActive() },
                    )
            }

            if (routine.description.isNotBlank()) {
                Text(routine.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp))
            }

            // Steps preview
            if (expanded && steps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                steps.forEach { step ->
                    Row(
                        modifier = Modifier.padding(start = 22.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RadioButtonUnchecked, null,
                            modifier = Modifier.size(14.dp),
                            tint = color)
                        Spacer(Modifier.width(8.dp))
                        Text(step.title, style = MaterialTheme.typography.bodySmall)
                        step.durationMinutes?.let {
                            Spacer(Modifier.width(4.dp))
                            Text("(${it}m)", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/collapse steps
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (expanded) "Hide steps" else "Show steps",
                        style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onStart, enabled = routine.isActive) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRoutineSheet(
    routineWithSteps: RoutineWithSteps?,
    onDismiss: () -> Unit,
    onSave: (Routine, List<RoutineStep>) -> Unit
) {
    val routine = routineWithSteps?.routine
    var name by remember { mutableStateOf(routine?.name ?: "") }
    var description by remember { mutableStateOf(routine?.description ?: "") }
    var colorHex by remember { mutableStateOf(routine?.colorHex ?: "#00838F") }
    var scheduledTime by remember { mutableStateOf(routine?.scheduledTime) }
    var selectedDays by remember {
        mutableStateOf(
            routine?.scheduledDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
        )
    }
    var steps by remember {
        mutableStateOf(
            routineWithSteps?.steps?.toMutableList() ?: mutableListOf()
        )
    }
    var showTimePicker by remember { mutableStateOf(false) }

    val colorOptions = listOf("#00838F", "#1565C0", "#2E7D32", "#E65100", "#C2185B", "#4527A0")
    val dayLabels = listOf(1 to "Mo", 2 to "Tu", 3 to "We", 4 to "Th", 5 to "Fr", 6 to "Sa", 7 to "Su")

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.95f)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (routine == null) "New Routine" else "Edit Routine",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Routine name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            // Color
            Text("Color", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colorOptions.forEach { hex ->
                    val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(if (colorHex == hex) 36.dp else 30.dp)
                            .clip(CircleShape)
                            .background(c)
                            .clickable { colorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (colorHex == hex) {
                            Icon(Icons.Default.Check, null, tint = Color.White,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Schedule
            Text("Auto-Schedule", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Select days to run automatically (leave empty for manual only)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayLabels.forEach { (num, label) ->
                    FilterChip(
                        selected = num in selectedDays,
                        onClick = {
                            selectedDays = if (num in selectedDays)
                                selectedDays - num else selectedDays + num
                        },
                        label = {
                            Text(
                                label,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            if (selectedDays.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(scheduledTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
                        ?: "Set start time (optional)")
                }
            }

            Divider()

            // Steps
            Text("Steps", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            steps.forEachIndexed { index, step ->
                StepEditor(
                    step = step,
                    index = index,
                    canMoveUp = index > 0,
                    canMoveDown = index < steps.size - 1,
                    onUpdate = { updated -> steps = steps.toMutableList().also { it[index] = updated } },
                    onDelete = { steps = steps.toMutableList().also { it.removeAt(index) } },
                    onMoveUp = {
                        if (index > 0) {
                            val list = steps.toMutableList()
                            val tmp = list[index]; list[index] = list[index - 1]; list[index - 1] = tmp
                            steps = list
                        }
                    },
                    onMoveDown = {
                        if (index < steps.size - 1) {
                            val list = steps.toMutableList()
                            val tmp = list[index]; list[index] = list[index + 1]; list[index + 1] = tmp
                            steps = list
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    steps = steps.toMutableList().also {
                        it.add(RoutineStep(routineId = routine?.id ?: 0, title = "", orderIndex = it.size))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Step")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        if (name.isNotBlank() && steps.any { it.title.isNotBlank() }) {
                            onSave(
                                (routine ?: Routine(name = "")).copy(
                                    name = name,
                                    description = description,
                                    colorHex = colorHex,
                                    scheduledTime = if (selectedDays.isEmpty()) null else scheduledTime,
                                    scheduledDays = selectedDays.sorted().joinToString(",")
                                ),
                                steps.filter { it.title.isNotBlank() }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }

    if (showTimePicker) {
        SimpleTimePickerDialog(
            initial = scheduledTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { scheduledTime = it; showTimePicker = false },
            onClear = { scheduledTime = null; showTimePicker = false }
        )
    }
}

@Composable
fun StepEditor(
    step: RoutineStep,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUpdate: (RoutineStep) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step ${index + 1}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedTextField(
                value = step.title,
                onValueChange = { onUpdate(step.copy(title = it)) },
                label = { Text("Step title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = step.durationMinutes?.toString() ?: "",
                    onValueChange = { onUpdate(step.copy(durationMinutes = it.toIntOrNull())) },
                    label = { Text("Duration (min)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Column(Modifier.weight(1f)) {
                    Text("Priority", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Priority.values().forEach { p ->
                            FilterChip(
                                selected = step.priority == p,
                                onClick = { onUpdate(step.copy(priority = p)) },
                                label = { Text(p.name.take(1), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            }
        }
    }
}


