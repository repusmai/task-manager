package com.personal.taskmanager.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            _updateState.value = UpdateState(message = "Couldn't check for updates. Check your connection.")
            return@launch
        }
        val available = updateChecker.isUpdateAvailable(context, release)
        _updateState.value = UpdateState(
            updateAvailable = available,
            latestVersion = release.tagName,
            apkUrl = release.apkUrl,
            message = if (!available) "You're on the latest version!" else null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val updateState by viewModel.updateState.collectAsState()
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
            // App info section
            SettingsSectionHeader("About")
            SettingsItem(
                icon = Icons.Default.Info,
                title = "App Version",
                subtitle = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) { "Unknown" }
            )

            Divider(Modifier.padding(horizontal = 16.dp))

            // Update section
            SettingsSectionHeader("Updates")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate,
                            null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Auto-Update", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Check GitHub for new versions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Status message
                    updateState.message?.let { msg ->
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Update available
                    if (updateState.updateAvailable) {
                        Text(
                            "New version available: ${updateState.latestVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Download progress
                    if (updateState.isDownloading) {
                        Text(
                            "Downloading... ${updateState.downloadProgress}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = updateState.downloadProgress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!updateState.updateAvailable && !updateState.isDownloading) {
                            Button(
                                onClick = viewModel::checkForUpdate,
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
                            OutlinedButton(
                                onClick = viewModel::checkForUpdate,
                                modifier = Modifier.weight(1f)
                            ) { Text("Re-check") }
                            Button(
                                onClick = viewModel::downloadAndInstall,
                                modifier = Modifier.weight(1f)
                            ) { Text("Install Update") }
                        }
                    }
                }
            }

            Divider(Modifier.padding(horizontal = 16.dp))

            // Notifications section
            SettingsSectionHeader("Notifications")
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Reminders",
                subtitle = "Manage in system notification settings"
            )
        }
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
