package com.littleetx.sustechnav.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.littleetx.sustechnav.data.NavigationData
import com.littleetx.sustechnav.data.NodeConnection
import com.littleetx.sustechnav.data.NodeInfo
import com.littleetx.sustechnav.data.getNode
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.material3.controls.DisappearingCompassButton
import dev.sargunv.maplibrecompose.material3.controls.DisappearingScaleBar
import dev.sargunv.maplibrecompose.material3.controls.ScaleBarMeasure
import dev.sargunv.maplibrecompose.material3.controls.ScaleBarMeasures
import io.github.dellisd.spatialk.geojson.Position

@Composable
fun AppMap(
    mode: MapMode,
    navData: NavigationData,
    modifier: Modifier = Modifier,
    isEditing: Boolean,
    setIsEditing: (Boolean) -> Unit,
    onModifyNode: (NodeInfo) -> Unit,
    onModifyConn: (NodeConnection) -> Unit,
    onDiscardEditing: () -> Unit,
) {
    var selectedFeature by remember { mutableStateOf<SelectedFeature?>(null) }
    var selectedNodeNewPosition by remember { mutableStateOf(Position(longitude = 0.0, latitude = 0.0))}

    var currentRoute by remember { mutableStateOf<SearchRouteResult>(SearchRouteResult()) }


    LaunchedEffect(mode) {
        if (mode != MapMode.Edit) {
            selectedFeature = null
        }
    }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 22.602, longitude = 113.995),
            zoom = 14.5,
        )
    )

    Box(modifier = modifier) {
        SustechMap(
            cameraState = cameraState,
            route = currentRoute,
            mode = mode,
            navData = navData,
            modifier = Modifier.fillMaxSize(),
            isEditing = isEditing,
            selectedFeature = selectedFeature,
            setSelectedFeature = { selectedFeature = it },
            selectedNodeNewPosition = selectedNodeNewPosition,
        )

        val drawDraggingIcon = isEditing && selectedFeature is SelectedNode
        if (drawDraggingIcon) {
            val current = cameraState.screenLocationFromPosition(selectedNodeNewPosition)
            val iconSize = 30.dp

            Icon(
                Icons.Filled.Place, contentDescription = "Draggable node",
                tint = Color.Black,
                modifier = Modifier
                    .size(iconSize)
                    .offset { IntOffset(x = (current.x - iconSize / 2).roundToPx(), y = (current.y - iconSize).roundToPx()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Note: 这个lambda不会随着selectedNodeNewPosition的改变而Recompose，必须使用代理，每次抓取selectedNodeNewPosition的最新值
                            val oldOffset = cameraState.screenLocationFromPosition(selectedNodeNewPosition)
                            val offset = DpOffset(x = dragAmount.x.toDp(), y = dragAmount.y.toDp())
                            selectedNodeNewPosition = cameraState.positionFromScreenLocation(oldOffset + offset)
                        }
                    }
                ,
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            when (mode) {
                MapMode.Edit -> {
                    val feature = selectedFeature
                    when (feature) {
                        is SelectedNode -> {
                            val node = navData.getNode(feature.id)!!
                            SelectedNodePanel(
                                node = node,
                                isEditing = isEditing,
                                setIsEditing = setIsEditing,
                                selectedNodeNewPosition = selectedNodeNewPosition,
                                setSelectedNodeNewPosition = { selectedNodeNewPosition = it },
                                onModifyNode = onModifyNode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .align(Alignment.BottomCenter)
                            )
                            if (isEditing) {
                                Text(
                                    "拖动标志修改节点位置",
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 110.dp)
                                )

                                CancelEditingButton(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    onClick = {
                                        if (node.position == selectedNodeNewPosition) {
                                            setIsEditing(false)
                                        } else {
                                            onDiscardEditing()
                                        }
                                    }
                                )
                            }
                        }
                        else -> {}
                    }
                }
                MapMode.Map -> {
                    RouteDestinationPanel(
                        navData = navData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                        ,
                        onCreateRoute = { currentRoute = it }
                    )
                }
            }

            DisappearingScaleBar(
                measures = ScaleBarMeasures(primary = ScaleBarMeasure.Metric),
                metersPerDp = cameraState.metersPerDpAtTarget,
                zoom = cameraState.position.zoom,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            DisappearingCompassButton(cameraState, modifier = Modifier.align(Alignment.TopStart))
        }
    }
}