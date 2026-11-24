package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SmileLightColorScheme = lightColorScheme(
    primary = SmileYellow,
    onPrimary = SmileBlack,
    primaryContainer = SmileYellowSoft,
    onPrimaryContainer = SmileBlack,
    secondary = SmileGreenOnline,
    onSecondary = SmileWhite,
    secondaryContainer = SmileGreenSoft,
    onSecondaryContainer = SmileBlack,
    tertiary = SmileYellowVibrant,
    onTertiary = SmileBlack,
    background = SmileWhite,
    onBackground = SmileBlack,
    surface = SmileWhite,
    onSurface = SmileBlack,
    surfaceVariant = SmileOffWhite,
    onSurfaceVariant = SmileMutedText,
    outline = SmileLightBorder,
    error = SmileRedCallEnd,
    onError = SmileWhite
)

private val SmileDarkColorScheme = darkColorScheme(
    primary = SmileYellow,
    onPrimary = SmileBlack,
    primaryContainer = SmileYellowDarkSurface,
    onPrimaryContainer = SmileYellow,
    secondary = SmileGreenOnline,
    onSecondary = SmileBlack,
    secondaryContainer = SmileYellowDarkSurface,
    onSecondaryContainer = SmileGreenOnline,
    tertiary = SmileYellowVibrant,
    onTertiary = SmileBlack,
    background = SmileDarkBackground,
    onBackground = SmileWhite,
    surface = SmileDarkSurface,
    onSurface = SmileWhite,
    surfaceVariant = SmileDarkCard,
    onSurfaceVariant = SmileDarkMutedText,
    outline = SmileDarkBorder,
    error = SmileRedCallEnd,
    onError = SmileWhite
)

val SmileShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmileTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SmileDarkColorScheme else SmileLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmileTypography,
        shapes = SmileShapes,
        content = content
    )
}
