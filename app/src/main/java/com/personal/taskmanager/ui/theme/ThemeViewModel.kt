package com.personal.taskmanager.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore("settings")
private val COLORWAY_KEY = stringPreferencesKey("colorway")
private val DARK_MODE_KEY = stringPreferencesKey("dark_mode")

data class ThemeState(
    val colorway: AppColorway = AppColorway.OCEAN_BLUE,
    val darkTheme: Boolean = false
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val themeState: StateFlow<ThemeState> = context.dataStore.data.map { prefs ->
        ThemeState(
            colorway = prefs[COLORWAY_KEY]?.let {
                runCatching { AppColorway.valueOf(it) }.getOrDefault(AppColorway.OCEAN_BLUE)
            } ?: AppColorway.OCEAN_BLUE,
            darkTheme = prefs[DARK_MODE_KEY] == "true"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeState())

    fun setColorway(colorway: AppColorway) = viewModelScope.launch {
        context.dataStore.edit { it[COLORWAY_KEY] = colorway.name }
    }

    fun setDarkTheme(dark: Boolean) = viewModelScope.launch {
        context.dataStore.edit { it[DARK_MODE_KEY] = dark.toString() }
    }
}
