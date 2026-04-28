package com.personal.taskmanager.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Ocean Blue (default) ──
private val OceanBlueLightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF1E88E5),
    tertiary = Color(0xFF00ACC1),
    background = Color(0xFFF5F8FF),
    surface = Color.White,
)
private val OceanBlueDarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003780),
    primaryContainer = Color(0xFF004BA0),
    secondary = Color(0xFF64B5F6),
    tertiary = Color(0xFF4DD0E1),
    background = Color(0xFF0D1B2A),
    surface = Color(0xFF162233),
)

// ── Forest Green ──
private val ForestGreenLightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F0BB),
    secondary = Color(0xFF43A047),
    tertiary = Color(0xFF00897B),
    background = Color(0xFFF4FAF4),
    surface = Color.White,
)
private val ForestGreenDarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003909),
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFA5D6A7),
    tertiary = Color(0xFF4DB6AC),
    background = Color(0xFF0D1F0E),
    surface = Color(0xFF162417),
)

// ── Sunset Orange ──
private val SunsetOrangeLightColors = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFFF7043),
    tertiary = Color(0xFFFDD835),
    background = Color(0xFFFFF8F5),
    surface = Color.White,
)
private val SunsetOrangeDarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF4A1800),
    primaryContainer = Color(0xFF7A2900),
    secondary = Color(0xFFFF8A65),
    tertiary = Color(0xFFFFF176),
    background = Color(0xFF1F1200),
    surface = Color(0xFF2C1A00),
)

// ── Rose Pink ──
private val RosePinkLightColors = lightColorScheme(
    primary = Color(0xFFC2185B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E6),
    secondary = Color(0xFFE91E8C),
    tertiary = Color(0xFF9C27B0),
    background = Color(0xFFFFF5F8),
    surface = Color.White,
)
private val RosePinkDarkColors = darkColorScheme(
    primary = Color(0xFFF48FB1),
    onPrimary = Color(0xFF650030),
    primaryContainer = Color(0xFF8F0045),
    secondary = Color(0xFFF06292),
    tertiary = Color(0xFFCE93D8),
    background = Color(0xFF200014),
    surface = Color(0xFF2D0020),
)

// ── Midnight Purple ──
private val MidnightPurpleLightColors = lightColorScheme(
    primary = Color(0xFF4527A0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEFF),
    secondary = Color(0xFF7B1FA2),
    tertiary = Color(0xFF1565C0),
    background = Color(0xFFF8F5FF),
    surface = Color.White,
)
private val MidnightPurpleDarkColors = darkColorScheme(
    primary = Color(0xFFB39DDB),
    onPrimary = Color(0xFF1A0066),
    primaryContainer = Color(0xFF311B92),
    secondary = Color(0xFFCE93D8),
    tertiary = Color(0xFF90CAF9),
    background = Color(0xFF100D1F),
    surface = Color(0xFF1A1530),
)

enum class AppColorway {
    OCEAN_BLUE, FOREST_GREEN, SUNSET_ORANGE, ROSE_PINK, MIDNIGHT_PURPLE, DYNAMIC
}

@Composable
fun TaskManagerTheme(
    darkTheme: Boolean = false,
    colorway: AppColorway = AppColorway.OCEAN_BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (colorway) {
        AppColorway.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) OceanBlueDarkColors else OceanBlueLightColors
            }
        }
        AppColorway.OCEAN_BLUE -> if (darkTheme) OceanBlueDarkColors else OceanBlueLightColors
        AppColorway.FOREST_GREEN -> if (darkTheme) ForestGreenDarkColors else ForestGreenLightColors
        AppColorway.SUNSET_ORANGE -> if (darkTheme) SunsetOrangeDarkColors else SunsetOrangeLightColors
        AppColorway.ROSE_PINK -> if (darkTheme) RosePinkDarkColors else RosePinkLightColors
        AppColorway.MIDNIGHT_PURPLE -> if (darkTheme) MidnightPurpleDarkColors else MidnightPurpleLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
