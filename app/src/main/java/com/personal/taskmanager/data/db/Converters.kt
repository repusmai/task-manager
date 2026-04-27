package com.personal.taskmanager.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter fun fromLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
    @TypeConverter fun toLocalDate(date: LocalDate?): String? = date?.toString()
    @TypeConverter fun fromLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }
    @TypeConverter fun toLocalTime(time: LocalTime?): String? = time?.toString()
}
