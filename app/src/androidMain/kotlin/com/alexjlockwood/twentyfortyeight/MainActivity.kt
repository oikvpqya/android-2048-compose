package com.alexjlockwood.twentyfortyeight

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalConfiguration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import okio.Path.Companion.toPath

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataStore = PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = null,
            migrations = emptyList(),
            produceFile = { application.filesDir.resolve(USER_DATA_FILE_NAME).absolutePath.toPath() },
        )

        if (VERSION.SDK_INT < VERSION_CODES.O) {
            enableEdgeToEdge()
        } else SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT).also {
            enableEdgeToEdge(it, it)
        }
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            App(
                dataStore = dataStore,
                isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
            )
        }
    }
}
