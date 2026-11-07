package app.goldapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern GoldApp palette — dark-first, gold accent.
val GoldPrimary = Color(0xFFD4AF37)
val GoldSecondary = Color(0xFFF6D365)
val GoldDeep = Color(0xFFB8860B)
val GoldGreen = Color(0xFF34C759)
val GoldRed = Color(0xFFFF453A)

private val GoldDarkColors = darkColorScheme(
    primary = GoldPrimary,
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

private val GoldLightColors = lightColorScheme(
    primary = Color(0xFF9A7B0A),
    onPrimary = Color.White,
    secondary = Color(0xFFB8860B),
    onSecondary = Color.White,
    tertiary = GoldDeep,
    background = Color(0xFFF7F5EF),
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFEFEDE6),
    onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFFD8D5CC),
    error = Color(0xFFD92D20),
    onError = Color.White
)

@Composable
fun GoldAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GoldDarkColors else GoldLightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
