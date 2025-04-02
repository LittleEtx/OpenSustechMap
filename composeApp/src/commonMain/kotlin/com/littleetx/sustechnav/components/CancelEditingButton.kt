package com.littleetx.sustechnav.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CancelEditingButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ElevatedButton(
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors()
            .copy(contentColor = MaterialTheme.colorScheme.error),
        onClick = onClick,
    ) {
        Icon(Icons.Filled.Close, contentDescription = "Cancel")
        Text("取消编辑")
    }
}