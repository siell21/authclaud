package siell.claud.authenticator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ElegantDarkColorScheme =
  darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = FabIconDark,
    secondary = TextSecondary,
    background = BackgroundDark,
    surface = BackgroundDark,
    surfaceVariant = CardBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextPrimary,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = ElegantDarkColorScheme, typography = Typography, content = content)
}
