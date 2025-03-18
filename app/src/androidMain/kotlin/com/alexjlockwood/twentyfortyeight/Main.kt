package com.alexjlockwood.twentyfortyeight

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.PolymorphismJson
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import io.github.xxfast.kstore.file.FileCodec
import io.github.xxfast.kstore.storeOf
import kotlinx.io.files.Path
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            val statusBarStyle = remember(isDarkTheme) {
                if (isDarkTheme) {
                    SystemBarStyle.dark(
                        Color.argb(0xff, 0x30, 0x3f, 0x9f),
                    )
                } else {
                    SystemBarStyle.light(
                        Color.argb(0xff, 0x00, 0x99, 0xcc),
                        Color.argb(0xff, 0x30, 0x3f, 0x9f),
                    )
                }
            }
            enableEdgeToEdge(statusBarStyle = statusBarStyle)
            App(repository = (application as GameRepositoryProvider).gameRepository)
        }
    }
}

class MainApplication : Application(), GameRepositoryProvider {

    override val gameRepository by lazy {
        val file = File(this.filesDir.absolutePath, USER_DATA_FILE_NAME).toString()
        DefaultGameRepository(
            store = storeOf(
                codec = FileCodec(
                    file = Path(file),
                    tempFile = Path("$file.temp"),
                    json = PolymorphismJson,
                    serializer = MapSerializer(String.serializer(), PolymorphicSerializer(Any::class)),
                ),
                default = null,
                enableCache = false,
            ),
        )
    }
}

private interface GameRepositoryProvider {

    val gameRepository: GameRepository
}
