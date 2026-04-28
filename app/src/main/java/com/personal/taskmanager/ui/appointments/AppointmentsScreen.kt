package com.personal.taskmanager.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.personal.taskmanager.data.model.Appointment
import com.personal.taskmanager.ui.tasks.DateStrip
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointments") },
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
                text = { Text("New Appointment") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            DateStrip(
                selectedDate = state.selectedDate,
                onDateSelected = viewModel::selectDate
            )

            if (state.appointments.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventNote, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No appointments for this day",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to add one",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.appointments, key = { it.id }) { appointment ->
                        AppointmentCard(
                            appointment = appointment,
                            onEdit = { appointmentToEdit = appointment },
                            onDelete = { viewModel.deleteAppointment(appointment) }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet || appointmentToEdit != null) {
        AddEditAppointmentSheet(
            appointment = appointmentToEdit,
            initialDate = state.selectedDate,
            onDismiss = { showAddSheet = false; appointmentToEdit = null },
            onSave = { appt ->
                if (appointmentToEdit != null) viewModel.updateAppointment(appt)
                else viewModel.addAppointment(appt)
                showAddSheet = false; appointmentToEdit = null
            }
        )
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(appointment.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val isMultiDay = appointment.startDate != appointment.endDate
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Row(Modifier.padding(12.dp)) {
            // Color bar
            Box(
                Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    appointment.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Date/time row
                val dateText = if (isMultiDay) {
                    "${appointment.startDate.format(dateFormatter)} → ${appointment.endDate.format(dateFormatter)}"
                } else {
                    appointment.startDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
                }
                val timeText = buildString {
                    appointment.startTime?.let { append(it.format(timeFormatter)) }
                    appointment.endTime?.let { append(" → ${it.format(timeFormatter)}") }
                }

                Text(
                    dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
                if (timeText.isNotBlank()) {
                    Text(
                        timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (appointment.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(2.dp))
                        Text(appointment.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (appointment.description.isNotBlank()) {
                    Text(appointment.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAppointmentSheet(
    appointment: Appointment?,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    var title by remember { mutableStateOf(appointment?.title ?: "") }
    var description by remember { mutableStateOf(appointment?.description ?: "") }
    var location by remember { mutableStateOf(appointment?.location ?: "") }
    var startDate by remember { mutableStateOf(appointment?.startDate ?: initialDate) }
    var endDate by remember { mutableStateOf(appointment?.endDate ?: initialDate) }
    var startTime by remember { mutableStateOf(appointment?.startTime) }
    var endTime by remember { mutableStateOf(appointment?.endTime) }
    var reminderMinutes by remember { mutableStateOf(appointment?.reminderMinutesBefore) }
    var colorHex by remember { mutableStateOf(appointment?.colorHex ?: "#6750A4") }

    val colorOptions = listOf(
        "#6750A4", "#1565C0", "#2E7D32", "#E65100", "#C2185B", "#00838F"
    )

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (appointment == null) "New Appointment" else "Edit Appointment",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
            )

            // Start date/time
            Text("Start", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(startDate.format(dateFormatter), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(startTime?.format(timeFormatter) ?: "No time")
                }
            }

            // End date/time
            Text("End", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(endDate.format(dateFormatter), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(endTime?.format(timeFormatter) ?: "No time")
                }
            }

            // Color picker
            Text("Color", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorOptions.forEach { hex ->
                    val c = try { Color(android.graphics.Color.parseColor(hex)) }
                           catch (e: Exception) { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(if (colorHex == hex) 36.dp else 30.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(c)
                            .clickable { colorHex = hex }
                    ) {
                        if (colorHex == hex) {
                            Icon(Icons.Default.Check, null,
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.Center).size(18.dp))
                        }
                    }
                }
            }

            // Reminder
            Text("Reminder", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null, 15, 30, 60, 1440).forEach { mins ->
                    FilterChip(
                        selected = reminderMinutes == mins,
                        onClick = { reminderMinutes = mins },
                        label = {
                            Text(when (mins) {
                                null -> "None"
                                1440 -> "1 day"
                                else -> "${mins}m before"
                            })
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isNotBlank() && !endDate.isBefore(startDate)) {
                            onSave(
                                (appointment ?: Appointment(
                                    title = "", startDate = startDate, endDate = endDate
                                )).copy(
                                    title = title,
                                    description = description,
                                    location = location,
                                    startDate = startDate,
                                    startTime = startTime,
                                    endDate = endDate,
                                    endTime = endTime,
                                    colorHex = colorHex,
                                    reminderMinutesBefore = reminderMinutes
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            initial = startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = {
                startDate = it
                if (endDate.isBefore(it)) endDate = it
                showStartDatePicker = false
            }
        )
    }
    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            initial = endDate,
            minDate = startDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { endDate = it; showEndDatePicker = false }
        )
    }

    // Time pickers
    if (showStartTimePicker) {
        SimpleTimePickerDialog(
            initial = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { startTime = it; showStartTimePicker = false },
            onClear = { startTime = null; showStartTimePicker = false }
        )
    }
    if (showEndTimePicker) {
        SimpleTimePickerDialog(
            initial = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { endTime = it; showEndTimePicker = false },
            onClear = { endTime = null; showEndTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initial: LocalDate,
    minDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    var day by remember { mutableStateOf(initial.dayOfMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Year
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Year: ", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { year-- }) { Icon(Icons.Default.Remove, null) }
                    Text("$year", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { year++ }) { Icon(Icons.Default.Add, null) }
                }
                // Month
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Month: ", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { if (month > 1) month-- else { month = 12; year-- } }) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text(LocalDate.of(year, month, 1)
                        .format(DateTimeFormatter.ofPattern("MMMM")), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { if (month < 12) month++ else { month = 1; year++ } }) {
                        Icon(Icons.Default.Add, null)
                    }
                }
                // Day
                val maxDay = java.time.YearMonth.of(year, month).lengthOfMonth()
                if (day > maxDay) day = maxDay
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Day: ", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { if (day > 1) day-- }) { Icon(Icons.Default.Remove, null) }
                    Text("$day", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { if (day < maxDay) day++ }) { Icon(Icons.Default.Add, null) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = LocalDate.of(year, month, day)
                if (minDate == null || !date.isBefore(minDate)) onConfirm(date)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTimePickerDialog(
    initial: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    onClear: () -> Unit
) {
    var hour by remember { mutableStateOf(initial?.hour ?: 9) }
    var minute by remember { mutableStateOf(initial?.minute ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { hour = (hour + 1) % 24 }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                    Text(String.format("%02d", hour), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                }
                Text(" : ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { minute = (minute + 5) % 60 }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                    Text(String.format("%02d", minute), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { minute = (minute - 5 + 60) % 60 }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) { Text("OK") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
