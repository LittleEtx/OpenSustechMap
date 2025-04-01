package com.littleetx.sustechnav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.compose.AppTheme
import com.littleetx.sustechnav.api.ApiPayload
import com.littleetx.sustechnav.components.MapMode
import com.littleetx.sustechnav.components.SustechMap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview


enum class AppMode {
    Map, EditMap
}

@Composable
fun AppBarButton(
    currentMode: AppMode,
    text: String,
    icon: ImageVector,
    buttonMode: AppMode,
    onClick: (AppMode) -> Unit,
) {
    TextButton(
        colors = ButtonDefaults.textButtonColors().copy(
            contentColor =
                if (currentMode == buttonMode) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.inversePrimary
        ),
        onClick = { onClick(buttonMode) }
    ) {
        Column {
            Icon(icon, contentDescription = text)
            Text(text)
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val apiPayload = remember { ApiPayload() }
    val coroutineScope = rememberCoroutineScope()
    val (appMode, setAppMode) = remember { mutableStateOf(AppMode.Map) }

    AppTheme {
        Scaffold(
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright)
                    ,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppBarButton(
                        currentMode = appMode,
                        onClick = setAppMode,
                        text = "地图",
                        icon = Icons.Filled.Map,
                        buttonMode = AppMode.Map,
                    )
                    AppBarButton(
                        currentMode = appMode,
                        onClick = setAppMode,
                        text = "编辑",
                        icon = Icons.Filled.EditLocation,
                        buttonMode = AppMode.EditMap,
                    )
                }
            },
        ) { innerPadding ->
            val mapMode = when(appMode) {
                AppMode.Map -> MapMode.Map
                AppMode.EditMap -> MapMode.Edit
            }
            SustechMap(
                mode = mapMode,
                apiPayload = apiPayload,
                coroutineScope = coroutineScope,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}