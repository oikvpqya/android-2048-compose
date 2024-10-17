package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.alexjlockwood.twentyfortyeight.domain.Artifact
import com.alexjlockwood.twentyfortyeight.Res
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AboutDialog(
    modifier: Modifier = Modifier,
    onDismissListener: () -> Unit,
) {
    var artifacts by remember { mutableStateOf(emptyList<Artifact>()) }
    Dialog(
        onDismissRequest = { onDismissListener() },
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onDismissListener() },
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Header(text = "About")
                }
                item {
                    About()
                }
                item {
                    Header(text = "Third party libraries")
                }
                items(artifacts) { item ->
                    Artifact(item)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        artifacts = Json.decodeFromString<List<Artifact>>(Res.readBytes("files/artifacts.json").decodeToString())
            .sortedBy { "${it.groupId}:${it.artifactId}" }
    }
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .padding(horizontal = 8.dp),
        text = text,
        style = MaterialTheme.typography.h6,
    )
}

@Composable
private fun About(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "2048 Compose",
            style = MaterialTheme.typography.h6,
        )
        Text(
            text = "Copyright (c) 2024 oikvpqya Yuya",
            style = MaterialTheme.typography.body2,
        )
        Text(
            text = "Copyright (c) 2020 Alex Lockwood",
            style = MaterialTheme.typography.body2,
        )
        Text(
            text = "Source https://github.com/oikvpqya/android-2048-compose",
            style = MaterialTheme.typography.body2,
        )
        Text(
            text = "MIT License",
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun Artifact(
    artifact: Artifact,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = artifact.name ?: artifact.artifactId,
            style = MaterialTheme.typography.h6,
        )
        Text(
            text = "${artifact.groupId}:${artifact.artifactId}:${artifact.version}",
            style = MaterialTheme.typography.body2,
        )
        artifact.spdxLicenses.forEach { item ->
            Text(
                text = item.name,
                style = MaterialTheme.typography.body2,
            )
        }
        artifact.unknownLicenses.forEach { item ->
            Text(
                text = item.name ?: item.url ?: "Unknown License",
                style = MaterialTheme.typography.body2,
            )
        }
    }
}
