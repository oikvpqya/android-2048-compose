package com.alexjlockwood.twentyfortyeight.repository

import com.alexjlockwood.twentyfortyeight.domain.DEFAULT_JSON
import com.alexjlockwood.twentyfortyeight.domain.UserData
import kotlinx.browser.localStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.w3c.dom.get
import org.w3c.dom.set

class DefaultGameRepository : GameRepository {

    private val lock = Mutex()

    override suspend fun fetch(): UserData {
        return lock.withLock {
            localStorage[USER_DATA_FILE_NAME]?.let { string ->
                DEFAULT_JSON.decodeFromString(string)
            } ?: UserData.EMPTY_USER_DATA
        }
    }

    override suspend fun update(data: UserData) {
        lock.withLock {
            localStorage[USER_DATA_FILE_NAME] = DEFAULT_JSON.encodeToString(data)
        }
    }
}
