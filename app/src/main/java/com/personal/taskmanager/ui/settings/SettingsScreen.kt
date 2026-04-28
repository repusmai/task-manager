package com.personal.taskmanager.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.taskmanager.ui.theme.AppColorway
import com.personal.taskmanager.ui.theme.ThemeViewModel
import com.personal.taskmanager.update.UpdateChecker
import com.personal.taskmanager.update.UpdateInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

data class UpdateState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val updateAvailable: Boolean = false,
    val latestVersion: String? = null,
    val apkUrl: String? = null,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val updateInstaller: UpdateInstaller,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState

    fun checkForUpdate() = viewModelScope.launch {
        _updateState.value = UpdateState(isChecking = true)
        val release = updateChecker.getLatestRelease()
        if (release == null) {
            _updateState.value = UpdateState(message = "Could not check for updates.")
            return@launch
        }
        val available = updateChecker.isUpdateAvailable(context, release)
        _updateState.value = UpdateState(
            updateAvailable = available,
            latestVersion = release.tagName,
            apkUrl = release.apkUrl,
            message = if (!available) "You are on the latest version!" else null
        )
    }

    fun downloadAndInstall() = viewModelScope.launch {
        val url = _updateState.value.apkUrl ?: return@launch
        _updateState.value = _updateState.value.copy(isDownloading = true)
        try {
            val file = updateInstaller.downloadApk(context, okHttpClient, url) { progress ->
                _updateState.value = _updateState.value.copy(downloadProgress = progress)
            }
            updateInstaller.installApk(context, file)
        } catch (e: Exception) {
            _updateState.value = UpdateState(message = "Download failed: ${e.message}")
        }
    }
}

// Color swatches for each colorway
private val colorwaySwatches = mapOf(
    AppColorway.OCEAN_BLUE     to Color(0xFF1565C0),
    AppColorway.FOREST_GREEN   to Color(0xFF2E7D32),
    AppColorway.SUNSET_ORANGE  to Color(0xFFE65100),
    AppColorway.ROSE_PINK      to Color(0xFFC2185B),
    AppColorway.MIDNIGHT_PURPLE to Color(0xFF4527A0),
    AppColorway.DYNAMIC        to Color(0xFF888888),
)

private val colorwayLabels = mapOf(
    AppColorway.OCEAN_BLUE      to "Ocean Blue",
    AppColorway.FOREST_GREEN    to "Forest Green",
    AppColorway.SUNSET_ORANGE   to "Sunset Orange",
    AppColorway.ROSE_PINK       to "Rose Pink",
    AppColorway.MIDNIGHT_PURPLE to "Midnight Purple",
    AppColorway.DYNAMIC         to "Dynamic (System)",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val updateState by settingsViewModel.updateState.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Appearance ──────────────────────────────────────────────
            SettingsSectionHeader("Appearance")

            // Dark mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DarkMode, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                    Text("Switch between light and dark",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = themeState.darkTheme,
                    onCheckedChange = { themeViewModel.setDarkTheme(it) }
                )
            }

            // Colorway picker
            Text(
                "Color Theme",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Two rows of colorway options
                val colorways = AppColorway.values().toList()
                colorways.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { colorway ->
                            ColorwayChip(
                                colorway = colorway,
                                isSelected = themeState.colorway == colorway,
                                onClick = { themeViewModel.setColorway(colorway) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if row is not full
                        repeat(3 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider(Modifier.padding(horizontal = 16.dp))

            // ── Updates ─────────────────────────────────────────────────
            SettingsSectionHeader("Updates")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Auto-Update", fontWeight = FontWeight.SemiBold)
                            Text("Check GitHub for new versions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    updateState.message?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (updateState.updateAvailable) {
                        Text("New version: ${updateState.latestVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (updateState.isDownloading) {
                        Text("Downloading... ${updateState.downloadProgress}%",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = updateState.downloadProgress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!updateState.updateAvailable && !updateState.isDownloading) {
                            Button(
                                onClick = settingsViewModel::checkForUpdate,
                                enabled = !updateState.isChecking,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (updateState.isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Check for Updates")
                                }
                            }
                        }
                        if (updateState.updateAvailable && !updateState.isDownloading) {
                            OutlinedButton(onClick = settingsViewModel::checkForUpdate,
                                modifier = Modifier.weight(1f)) { Text("Re-check") }
                            Button(onClick = settingsViewModel::downloadAndInstall,
                                modifier = Modifier.weight(1f)) { Text("Install Update") }
                        }
                    }
                }
            }

            Divider(Modifier.padding(horizontal = 16.dp))

            // ── About ────────────────────────────────────────────────────
            SettingsSectionHeader("About")
            SettingsItem(
                icon = Icons.Default.Info,
                title = "App Version",
                subtitle = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) { "Unknown" }
            )
        }
    }
}

@Composable
fun ColorwayChip(
    colorway: AppColorway,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatch = colorwaySwatches[colorway] ?: Color.Gray
    val label = colorwayLabels[colorway] ?: colorway.name

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(swatch)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
