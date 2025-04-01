package com.littleetx.sustechnav.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.littleetx.sustechnav.NodeId
import com.littleetx.sustechnav.data.NavigationData
import com.littleetx.sustechnav.data.NodeConnection
import com.littleetx.sustechnav.data.NodeInfo
import com.littleetx.sustechnav.data.getNode
import com.littleetx.sustechnav.data.notContainsNode
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.layer.Anchor
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.LineLayer
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.OrnamentSettings
import dev.sargunv.maplibrecompose.expressions.dsl.const
import dev.sargunv.maplibrecompose.material3.controls.DisappearingCompassButton
import dev.sargunv.maplibrecompose.material3.controls.DisappearingScaleBar
import dev.sargunv.maplibrecompose.material3.controls.ScaleBarMeasure
import dev.sargunv.maplibrecompose.material3.controls.ScaleBarMeasures
import io.github.dellisd.spatialk.geojson.Feature
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.GeoJson
import io.github.dellisd.spatialk.geojson.LineString
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sustechnav.composeapp.generated.resources.Res
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds


const val FEATURE_TYPE = "custom-map-data-type"
const val FEATURE_VALUE = "custom-map-data-value"

sealed class SelectedFeature
@Serializable
class SelectedNode(val id: NodeId) : SelectedFeature()

@Serializable
class SelectedConnection(val node1: NodeId, val node2: NodeId) : SelectedFeature()

val nodeType = JsonPrimitive("node")
val connectionType = JsonPrimitive("connection")


@OptIn(ExperimentalResourceApi::class)
@Composable
fun getMapEditGeoJson(navData: NavigationData): State<GeoJson> {
    return produceState<GeoJson>(initialValue = FeatureCollection(), navData) {
        val nodeFeatures = navData.nodes.values.map {
            Feature(
                id = "node-${it.id}",
                geometry = Point(coordinates = it.position),
                properties = mapOf(
                    FEATURE_TYPE to nodeType,
                    FEATURE_VALUE to Json.encodeToJsonElement(SelectedNode(it.id))
                ),
            )
        }

        val lines = mutableSetOf<Pair<NodeId, NodeId>>()
        navData.nodeConn.values.forEach { connections ->
            connections.forEach {
                if (navData.notContainsNode(it.target)) {
                    Logger.w { "Node ${it.target} not in navData!" }
                    return@forEach
                }
                if (navData.notContainsNode(it.origin)) {
                    Logger.w { "Node ${it.origin} not in navData!" }
                    return@forEach
                }
                val pair = it.origin to it.target
                val pairRevered = it.target to it.origin
                if (pair !in lines && pairRevered !in lines) {
                    lines.add(pair)
                }
            }
        }

        val lineFeatures = lines.map { (node1, node2) ->
            Feature(
                id = "conn-${node1}-${node2}",
                geometry = LineString(
                    navData.getNode(node1)!!.position,
                    navData.getNode(node2)!!.position,
                ),
                properties = mapOf(
                    FEATURE_TYPE to connectionType,
                    FEATURE_VALUE to Json.encodeToJsonElement(SelectedConnection(node1 = node1, node2 = node2))
                ),
            )
        }

        value = FeatureCollection(nodeFeatures + lineFeatures)
        Logger.i { "Updated value" }
    }
}

enum class MapMode {
    Map, Edit
}

val MapMode.isEditMode get() = this == MapMode.Edit

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SustechMap(
    mode: MapMode,
    navData: NavigationData,
    onModifyNode: (NodeInfo) -> Unit,
    onModifyConn: (NodeConnection) -> Unit,
    modifier: Modifier = Modifier,
    isEditingNode: Boolean,
    setIsEditingNode: (Boolean) -> Unit,
    onDiscardEditing: () -> Unit,
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 22.602, longitude = 113.995),
            zoom = 14.5,
        )
    )

    val styleState = rememberStyleState()
    var selectedFeature by remember { mutableStateOf<SelectedFeature?>(null) }
    var selectedNodeNewPosition by remember { mutableStateOf(Position(longitude = 0.0, latitude = 0.0))}

    LaunchedEffect(selectedFeature) {
        val feature = selectedFeature
        if (feature != null && feature is SelectedNode) {
            cameraState.animateTo(
                finalPosition = cameraState.position.copy(
                    target = navData.getNode(feature.id)!!.position,
                    zoom = max(cameraState.position.zoom, 18.0)
                ),
                duration = 0.5.seconds,
            )
        }
    }

    LaunchedEffect(mode) {
        if (mode != MapMode.Edit) {
            selectedFeature = null
        }
    }


    Box(modifier = modifier) {
        MaplibreMap(
            cameraState = cameraState,
            styleState = styleState,
            styleUri = Res.getUri("files/osm_liberty.json"),
            ornamentSettings = OrnamentSettings.AllDisabled,
            onMapClick = { pos, offset ->
                if (isEditingNode) {
                    return@MaplibreMap ClickResult.Pass
                }

                val feature = cameraState.queryRenderedFeatures(offset)
                    .filter { FEATURE_TYPE in it.properties }
                    .maxByOrNull {
                        when(it.properties[FEATURE_TYPE]) {
                            nodeType -> 3
                            connectionType -> 2
                            else-> 0
                        }
                    }

                if (feature == null) {
                    selectedFeature = null
                    return@MaplibreMap ClickResult.Pass
                }

                when (feature.properties[FEATURE_TYPE]) {
                    nodeType -> {
                        val point = feature.geometry as Point
                        Logger.i { "Point: id = ${feature.id}, lat = ${point.coordinates.latitude}, lon = ${point.coordinates.longitude}" }
                        selectedFeature = Json.decodeFromJsonElement<SelectedNode>(feature.properties[FEATURE_VALUE]!!)
                    }
                    else -> return@MaplibreMap ClickResult.Pass
                }
                ClickResult.Consume
            },
        ) {

            val nodePoints = rememberGeoJsonSource(
                id = "node-points",
                data = getMapEditGeoJson(navData).value,
            )

            val feature = selectedFeature
            val selectingPoint = rememberGeoJsonSource(
                id = "selecting-point",
                data = if (feature != null && feature is SelectedNode) {
                    val node = navData.getNode(feature.id)
                    Feature(
                        geometry = Point(coordinates = node!!.position),
                    )
                } else FeatureCollection()
            )

            val editingPointLine = rememberGeoJsonSource(
                id = "editing-point",
                data = if (isEditingNode) {
                    val node = navData.getNode((feature as SelectedNode).id)
                    Feature(
                        geometry = LineString(
                            coordinates = listOf(
                                node!!.position,
                                selectedNodeNewPosition,
                            ),
                        ),
                    )
                } else FeatureCollection(),
            )

            Anchor.Below("building-3d") {
                CircleLayer(
                    id = "node-points",
                    visible = mode.isEditMode,
                    source = nodePoints,
                    radius = const(5.dp),
                    color = const(Color.Blue),
                )
                LineLayer(
                    id = "node-connections",
                    visible = mode.isEditMode,
                    source = nodePoints,
                    width = const(5.dp),
                    color = const(Color.Blue),
                )

                LineLayer(
                    id = "editing-point-line",
                    visible = isEditingNode,
                    source = editingPointLine,
                    width = const(5.dp),
                    color = const(Color.Green),
                    dasharray = const(listOf(2, 1))
                )

                CircleLayer(
                    id = "editing-point-green",
                    visible = isEditingNode,
                    source = editingPointLine,
                    radius = const(5.dp),
                    color = const(Color.Green),
                    strokeWidth = const(1.dp),
                    strokeColor = const(Color.Black)
                )

                CircleLayer(
                    id = "selecting-point",
                    visible = selectedFeature != null,
                    source = selectingPoint,
                    radius = const(5.dp),
                    color = const(Color.Red),
                    strokeWidth = const(1.dp),
                    strokeColor = const(Color.Black)
                )
            }
        }

        val drawDraggingIcon = isEditingNode && selectedFeature != null
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
                            val oldOffset = cameraState.screenLocationFromPosition(selectedNodeNewPosition)
                            selectedNodeNewPosition = cameraState.positionFromScreenLocation(
                                oldOffset + DpOffset(x = dragAmount.x.toDp(), y = dragAmount.y.toDp())
                            )
                        }
                    }
                ,
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val feature = selectedFeature
            if (mode.isEditMode && feature != null && feature is SelectedNode) {
                val node = navData.getNode(feature.id)!!

                if (isEditingNode) {
                    ElevatedButton(
                        modifier = Modifier.align(Alignment.TopEnd),
                        colors = ButtonDefaults.outlinedButtonColors()
                            .copy(contentColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            if (node.position == selectedNodeNewPosition) {
                                setIsEditingNode(false)
                            } else {
                                onDiscardEditing()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        Text("取消编辑")
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (!isEditingNode) {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onClick = { expanded = !expanded }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")

                                DropdownMenu(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = "Edit") },
                                        text = { Text("编辑节点") },
                                        onClick = {
                                            setIsEditingNode(true)
                                            selectedNodeNewPosition = node.position
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = "Delete") },
                                        text = { Text("删除节点") },
                                        colors = MenuDefaults.itemColors().copy(
                                            textColor = MaterialTheme.colorScheme.error,
                                            leadingIconColor = MaterialTheme.colorScheme.error,
                                        ),
                                        onClick = {
                                            //TODO
                                        }
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onClick = {
                                    setIsEditingNode(false)
                                    onModifyNode(NodeInfo(
                                        id = node.id,
                                        position = selectedNodeNewPosition,
                                    ))
                                }
                            ) {
                                Icon(Icons.Filled.Done, contentDescription = "Done")
                            }
                        }

                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("节点 ID: ${node.id}")
                            Row {
                                Text("经度：${"%3.7f".sprintf(node.position.latitude)}")
                                if (isEditingNode) {
                                    Icon(Icons.Filled.KeyboardDoubleArrowRight,
                                        contentDescription = "To",
                                        tint = Color.Green,
                                    )
                                    Text("%3.7f".sprintf(selectedNodeNewPosition.latitude),
                                        color = Color.Green,
                                    )
                                }
                            }
                            Row {
                                Text("纬度：${"%3.7f".sprintf(node.position.longitude)}")
                                if (isEditingNode) {
                                    Icon(Icons.Filled.KeyboardDoubleArrowRight,
                                        contentDescription = "To",
                                        tint = Color.Green,
                                    )
                                    Text("%3.7f".sprintf(selectedNodeNewPosition.longitude),
                                        color = Color.Green,
                                    )
                                }
                            }
                        }
                    }
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