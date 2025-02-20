package com.alexjlockwood.twentyfortyeight.ui

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GameDialog(
    title: String,
    message: String,
    onConfirmListener: () -> Unit,
    onDismissListener: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = { TextButton(onClick = { onConfirmListener() }) { Text("OK") } },
        dismissButton = { TextButton(onClick = { onDismissListener() }) { Text("Cancel") } },
        onDismissRequest = { onDismissListener() },
    )
}
