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
            // Month header
            val visibleMonth = calendarState.firstVisibleMonth.yearMonth
            Text(
                visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Day of week headers
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalCalendar(
                state = calendarState,
                dayContent = { day ->
                    // Count tasks for this day — simplified: use all tasks list
                    val hasTasks = state.tasks.any { it.dueDate == day.date }
                    val hasOverdue = state.tasks.any {
                        it.dueDate == day.date && it.status == TaskStatus.OVERDUE
                    }
                    CalendarDayCell(
                        day = day,
                        isSelected = day.date == state.selectedDate,
                        hasTasks = hasTasks,
                        hasOverdue = hasOverdue,
                        onClick = {
                            if (day.position == DayPosition.MonthDate) {
                                viewModel.selectDate(day.date)
                                onNavigateBack()
                            }
                        }
                    )
                }
            )

            Divider(Modifier.padding(vertical = 8.dp))

            // Tasks for selected date
            Text(
                state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (state.tasks.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.tasks) { task ->
                        CalendarTaskRow(task)
                    }
                }
            }
        }
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
