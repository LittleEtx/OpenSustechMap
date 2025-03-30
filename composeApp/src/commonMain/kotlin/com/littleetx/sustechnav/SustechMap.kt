package com.littleetx.sustechnav

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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.layer.Anchor
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.LineLayer
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.GestureSettings
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sustechnav.composeapp.generated.resources.Res
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds


const val CUSTOM_MAP_DATA_KEY = "custom-map-data"


@OptIn(ExperimentalResourceApi::class)
@Composable
fun loadNodeGeoJson(): State<GeoJson> {
    return produceState<GeoJson>(initialValue = FeatureCollection()) {
        val bytes = Res.readBytes("files/map_data.json")
        val mapData = Json.decodeFromString<MapData>(bytes.decodeToString())
        val features = mapData.nodes.map {
            Feature(
                id = it.id,
                geometry = Point(
                    coordinates = Position(longitude = it.lon, latitude = it.lat)
                ),
                properties = mapOf(CUSTOM_MAP_DATA_KEY to JsonPrimitive("node")),
            )
        }
        value = FeatureCollection(features)
        Logger.i { "Updated value" }
    }
}


fun updateNode(newNode: MapNode) {

}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SustechMap(modifier: Modifier = Modifier) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 22.602, longitude = 113.995),
            zoom = 14.5,
        )
    )

    val styleState = rememberStyleState()

    var isInsightMode by remember { mutableStateOf(false) }
    var isDragEnable by remember { mutableStateOf(true) }
    var selectedNode by remember { mutableStateOf<MapNode?>(null) }
    var selectedNodeNewPosition by remember { mutableStateOf(Position(longitude = 0.0, latitude = 0.0))}
    var isEditingNode by remember { mutableStateOf(false) }

    LaunchedEffect(selectedNode) {
        selectedNode?.let {
            isEditingNode = false
            cameraState.animateTo(
                finalPosition = cameraState.position.copy(
                    target = Position(
                        longitude = it.lon,
                        latitude = it.lat,
                    ),
                    zoom = max(cameraState.position.zoom, 18.0)
                ),
                duration = 0.5.seconds,
            )
        }
    }

    Box(modifier = modifier) {
        MaplibreMap(
            cameraState = cameraState,
            styleState = styleState,
            styleUri = Res.getUri("files/osm_liberty.json"),
            ornamentSettings = OrnamentSettings.AllDisabled,
            gestureSettings = GestureSettings(isScrollGesturesEnabled = isDragEnable),
            onMapClick = { pos, offset ->
                val feature = cameraState.queryRenderedFeatures(offset)
                    .firstOrNull { CUSTOM_MAP_DATA_KEY in it.properties }

                if (feature == null) {
                    selectedNode = null
                    return@MaplibreMap ClickResult.Pass
                }

                when (feature.properties[CUSTOM_MAP_DATA_KEY]) {
                    JsonPrimitive("node") -> {
                        val point = feature.geometry as Point
                        Logger.i { "Point: id = ${feature.id}, lat = ${point.coordinates.latitude}, lon = ${point.coordinates.longitude}" }
                        selectedNode = MapNode(
                            id = feature.id ?: "".also { Logger.w { "Null id for selected point feature!" } },
                            lat = point.coordinates.latitude,
                            lon = point.coordinates.longitude,
                        )
                    }
                    else -> return@MaplibreMap ClickResult.Pass
                }
                ClickResult.Consume
            },
        ) {

            val nodePoints = rememberGeoJsonSource(
                id = "node-points",
                data = loadNodeGeoJson().value,
            )
            val selectingPoint = rememberGeoJsonSource(
                id = "selecting-point",
                data = if (selectedNode != null) {
                    Feature(
                        geometry = Point(
                            coordinates = Position(longitude = selectedNode!!.lon, latitude = selectedNode!!.lat)
                        ),
                    )
                } else FeatureCollection()
            )

            val editingPointLine = rememberGeoJsonSource(
                id = "editing-point",
                data = if (isEditingNode) {
                    Feature(
                        geometry = LineString(
                            coordinates = listOf(
                                Position(longitude = selectedNode!!.lon, latitude = selectedNode!!.lat),
                                selectedNodeNewPosition,
                            ),
                        ),
                    )
                } else FeatureCollection(),
            )

            Anchor.Below("building-3d") {
                CircleLayer(
                    id = "node-points",
                    visible = isInsightMode,
                    source = nodePoints,
                    radius = const(5.dp),
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
                    visible = selectedNode != null,
                    source = selectingPoint,
                    radius = const(5.dp),
                    color = const(Color.Red),
                    strokeWidth = const(1.dp),
                    strokeColor = const(Color.Black)
                )
            }
        }

        val drawDraggingIcon = isEditingNode && selectedNode != null
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
                            Logger.i { "$dragAmount" }
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

            Button(
                onClick = { isInsightMode = !isInsightMode },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 20.dp),
            ) {
                Text(if (isInsightMode) { "关闭编辑模式" } else { "开启编辑模式" })
            }

            if (isInsightMode && selectedNode != null) {
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
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onClick = {
                                    isEditingNode = true
                                    selectedNodeNewPosition = Position(
                                        longitude = selectedNode!!.lon,
                                        latitude = selectedNode!!.lat,
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                        } else {
                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onClick = {
                                    isEditingNode = false
                                    updateNode(MapNode(
                                        id = selectedNode!!.id,
                                        lon = selectedNodeNewPosition.longitude,
                                        lat = selectedNodeNewPosition.latitude,
                                    ))
                                }
                            ) {
                                Icon(Icons.Filled.Done, contentDescription = "Done")
                            }
                        }

                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("节点 ID: ${selectedNode!!.id}")
                            Row {
                                Text("经度：${"%3.7f".sprintf(selectedNode!!.lat)}")
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
                                Text("纬度：${"%3.7f".sprintf(selectedNode!!.lon)}")
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
                modifier = Modifier.align(Alignment.TopStart),
            )
            DisappearingCompassButton(cameraState, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}