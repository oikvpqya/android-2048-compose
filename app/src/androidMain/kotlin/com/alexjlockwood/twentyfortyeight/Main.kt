package com.alexjlockwood.twentyfortyeight

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.alexjlockwood.twentyfortyeight.domain.UserData
import com.alexjlockwood.twentyfortyeight.repository.DefaultGameRepository
import com.alexjlockwood.twentyfortyeight.repository.GameRepository
import com.alexjlockwood.twentyfortyeight.repository.USER_DATA_FILE_NAME
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(repository = (application as GameRepositoryProvider).gameRepository)
        }
    }
}

class MainApplication : Application(), GameRepositoryProvider {

    override val gameRepository by lazy {
        DefaultGameRepository(
            store = storeOf(
                file = Path(File(this.filesDir.absolutePath, USER_DATA_FILE_NAME).toString()),
                default = UserData.EMPTY_USER_DATA,
            ),
        )
    }
}

private interface GameRepositoryProvider {

    val gameRepository: GameRepository
}
