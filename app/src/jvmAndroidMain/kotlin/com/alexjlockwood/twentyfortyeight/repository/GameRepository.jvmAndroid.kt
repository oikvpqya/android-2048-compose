package com.alexjlockwood.twentyfortyeight.repository

import com.alexjlockwood.twentyfortyeight.domain.DEFAULT_JSON
import com.alexjlockwood.twentyfortyeight.domain.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.io.decodeFromSource
import kotlinx.serialization.json.io.encodeToSink
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.moveTo
import kotlin.io.path.outputStream

class DefaultGameRepository(
    private val file: Path,
) : GameRepository {

    private val coroutineContext = Dispatchers.IO + Job()
    private val lock = Mutex()

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun fetch(): UserData {
        return withContext(coroutineContext) {
            lock.withLock {
                if (file.exists()) {
                    file.inputStream().asSource().buffered().use { source ->
                        DEFAULT_JSON.decodeFromSource(source)
                    }
                } else {
                    UserData.EMPTY_USER_DATA
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun update(data: UserData) {
        withContext(coroutineContext) {
            lock.withLock {
                val tempFile = createTempFile()
                try {
                    tempFile.outputStream().asSink().buffered().use { sink ->
                        DEFAULT_JSON.encodeToSink(data, sink)
                    }
                } catch (exception: Throwable) {
                    tempFile.deleteIfExists()
                    throw exception
                }
                tempFile.moveTo(file, ATOMIC_MOVE, REPLACE_EXISTING)
            }
        }
    }
}
