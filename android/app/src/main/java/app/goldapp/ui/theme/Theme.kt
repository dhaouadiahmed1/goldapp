package app.goldapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// GoldApp v0.3 palette — light finance template, gold accent.
// Light is the main look; dark follows the system for dark-mode users.
val GoldPrimary = Color(0xFF9A7B0A) // dark gold: readable as text on white
val GoldAccent = Color(0xFFD4AF37) // bright gold: chart line, filled buttons
val GoldSecondary = Color(0xFFF6D365)
val GoldDeep = Color(0xFFB8860B)
val GoldGreen = Color(0xFF2E9E5B)
val GoldRed = Color(0xFFE5484D)

private val GoldLightColors = lightColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.White,
    secondary = GoldAccent,
    onSecondary = Color(0xFF111111),
    tertiary = GoldDeep,
    background = Color(0xFFF6F6F8),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEFEFF2),
    onSurfaceVariant = Color(0xFF6B6B70),
    outline = Color(0xFFE2E2E8),
    error = GoldRed,
    onError = Color.White
)

private val GoldDarkColors = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color(0xFF111111),
    secondary = GoldSecondary,
    onSecondary = Color(0xFF111111),
    tertiary = GoldDeep,
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF26262A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF3A3A3E),
    error = GoldRed,
    onError = Color.White
)

@Composable
fun GoldAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GoldDarkColors else GoldLightColors
    // Light-first template; dark follows the system with the v0.2 dark colours.
    MaterialTheme(colorScheme = colorScheme, content = content)
}
