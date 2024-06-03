package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = getColorScheme(isDarkTheme, isDynamicColor),
        content = content,
    )
}

@Composable
expect fun getColorScheme(
    isDarkTheme: Boolean,
    isDynamicColor: Boolean
): ColorScheme
