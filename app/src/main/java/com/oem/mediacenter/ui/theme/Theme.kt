package com.oem.mediacenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val BrandPrimary = Color(0xFF2F6FED)
val BrandSurface = Color(0xFF0F1720)
val BrandOnSurface = Color(0xFFF2F5F8)
val BrandSurfaceVariant = Color(0xFF1B2A3A)
val BrandMuted = Color(0xFF9AA7B5)

private val ColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnSurface,
    background = BrandSurface,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandMuted,
)

val TouchMin = 64.dp

@Composable
fun MediaCenterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content,
    )
}
