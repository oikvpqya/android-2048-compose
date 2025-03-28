package com.alexjlockwood.twentyfortyeight.domain

import kotlinx.serialization.json.Json

val DEFAULT_JSON: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
