package com.littleetx.sustechnav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.example.compose.AppTheme
import com.littleetx.sustechnav.api.ApiPayload
import com.littleetx.sustechnav.api.Failure
import com.littleetx.sustechnav.api.Success
import com.littleetx.sustechnav.api.getMapData
import com.littleetx.sustechnav.api.updateNode
import com.littleetx.sustechnav.components.MapMode
import com.littleetx.sustechnav.components.SustechMap
import com.littleetx.sustechnav.data.NavigationData
import com.littleetx.sustechnav.data.toNavigationData
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview
import sustechnav.composeapp.generated.resources.Res


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
fun fetchNavData(
    apiPayload: ApiPayload,
    updateHandle: Int,
    setProcessing: (Boolean) -> Unit,
    onFailure: (Failure<*>) -> Unit
) : State<NavigationData> {
    return produceState(initialValue = NavigationData(), updateHandle) {
        setProcessing(true)
        val result = apiPayload.getMapData()
        val mapData = when(result) {
            is Success -> result.value
            is Failure -> {
                Logger.w { "Fail to get map data, will use the default map data instead" }
                onFailure(result)
                val bytes = Res.readBytes("files/map_data.json")
                Json.decodeFromString<MapData>(bytes.decodeToString())
            }
        }
        value = mapData.toNavigationData()
        setProcessing(false)
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val apiPayload = remember { ApiPayload() }

    var updateNavDataHandle by remember { mutableStateOf(0) }
    val (appMode, setAppMode) = remember { mutableStateOf(AppMode.Map) }
    val (isProcessingRequest, setIsProcessing) = remember { mutableStateOf(false) }
    val (isEditingNode, setIsEditingNode) = remember { mutableStateOf(false) }


    val navData by fetchNavData(
        apiPayload = apiPayload,
        updateHandle = updateNavDataHandle,
        setProcessing = setIsProcessing,
        onFailure = {
            scope.launch {
                snackbarHostState.showSnackbar(message = it.message)
            }
        }
    )


    AppTheme {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            bottomBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                val mapMode = when(appMode) {
                    AppMode.Map -> MapMode.Map
                    AppMode.EditMap -> MapMode.Edit
                }
                SustechMap(
                    modifier = Modifier.fillMaxSize(),
                    mode = mapMode,
                    navData = navData,
                    onModifyNode = { node ->
                        scope.launch {
                            setIsProcessing(true)
                            val result = apiPayload.updateNode(MapNode(
                                id = node.id,
                                lon = node.position.longitude,
                                lat = node.position.latitude,
                            ))
                            when (result) {
                                is Success<*> -> ++updateNavDataHandle
                                is Failure<*> -> snackbarHostState.showSnackbar(result.message)
                            }
                            setIsProcessing(false)
                        }
                    },
                    onModifyConn = {

                    },
                    isEditingNode = isEditingNode,
                    setIsEditingNode = setIsEditingNode,
                )


                AnimatedVisibility(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .align(Alignment.TopCenter)
                    ,
                    visible = isProcessingRequest
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape,
                            )
                        ,
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(26.dp)
                                .align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}