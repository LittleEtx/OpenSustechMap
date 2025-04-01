package com.littleetx.sustechnav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.littleetx.sustechnav.api.ApiPayload
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val apiPayload = remember { ApiPayload() }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        SustechMap(
            apiPayload = apiPayload,
            scope = scope,
            modifier = Modifier.fillMaxSize(),
        )
    }
}