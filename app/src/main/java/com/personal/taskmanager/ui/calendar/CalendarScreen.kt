package com.personal.taskmanager.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.personal.taskmanager.data.model.Priority
import com.personal.taskmanager.data.model.Task
import com.personal.taskmanager.data.model.TaskStatus
import com.personal.taskmanager.ui.tasks.TasksViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val calendarState = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(6),
        endMonth = YearMonth.now().plusMonths(12),
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = firstDayOfWeekFromLocale()
    )

    val coroutineScope = rememberCoroutineScope()
    var showYearPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Month navigation header
            val visibleMonth = calendarState.firstVisibleMonth.yearMonth
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(visibleMonth.minusMonths(1))
                    }
                }) { Icon(Icons.Default.ArrowBackIosNew, "Previous month") }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showYearPicker = true }
                ) {
                    Text(visibleMonth.format(DateTimeFormatter.ofPattern("MMMM")),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(visibleMonth.year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }

                IconButton(onClick = {
                    coroutineScope.launch {
                        calendarState.animateScrollToMonth(visibleMonth.plusMonths(1))
                    }
                }) { Icon(Icons.Default.ArrowForwardIos, "Next month") }
            }

            // Day of week headers
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalCalendar(
                state = calendarState,
                dayContent = { day ->
                    val hasTasks = state.allTasks.any { it.dueDate == day.date }
                    val hasOverdue = state.allTasks.any {
                        it.dueDate == day.date && it.status == TaskStatus.OVERDUE
                    }
                    CalendarDayCell(
                        day = day,
                        isSelected = day.date in state.selectedDates,
                        hasTasks = hasTasks,
                        hasOverdue = hasOverdue,
                        onClick = {
                            if (day.position == DayPosition.MonthDate) {
                                viewModel.toggleDate(day.date)
                                onNavigateBack()
                            }
                        }
                    )
                }
            )

            Divider(Modifier.padding(vertical = 8.dp))

            val displayDate = state.selectedDates.minOrNull() ?: LocalDate.now()
            Text(
                displayDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
            )

            val tasksForDisplay = if (state.selectedDates.isEmpty())
                emptyList()
            else
                state.allTasks.filter { it.dueDate in state.selectedDates }

            if (tasksForDisplay.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No tasks", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tasksForDisplay) { task -> CalendarTaskRow(task) }
                }
            }
        }
    }

    // Year picker dialog
    if (showYearPicker) {
        val years = (YearMonth.now().year - 10..YearMonth.now().year + 10).toList()
        val visibleMonth = calendarState.firstVisibleMonth.yearMonth
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            title = { Text("Jump to year") },
            text = {
                LazyColumn {
                    items(years) { year ->
                        Text(
                            year.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        calendarState.animateScrollToMonth(YearMonth.of(year, visibleMonth.month))
                                    }
                                    showYearPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (year == visibleMonth.year) FontWeight.Bold else FontWeight.Normal,
                            color = if (year == visibleMonth.year) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearPicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    hasTasks: Boolean,
    hasOverdue: Boolean,
    onClick: () -> Unit
) {
    val isToday = day.date == LocalDate.now()
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !isCurrentMonth -> MaterialTheme.colorScheme.outlineVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasTasks && isCurrentMonth) {
                Box(
                    Modifier.size(4.dp).clip(CircleShape).background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else if (hasOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
fun CalendarTaskRow(task: Task) {
    val dotColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> MaterialTheme.colorScheme.primary
        Priority.LOW -> MaterialTheme.colorScheme.tertiary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Circle, null,
            modifier = Modifier.size(8.dp), tint = dotColor)
        Spacer(Modifier.width(10.dp))
        Text(task.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        task.dueTime?.let {
            Text(
                it.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
