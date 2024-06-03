package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun getColorScheme(isDarkTheme: Boolean, isDynamicColor: Boolean) = if (isDarkTheme) darkColorScheme() else lightColorScheme()
