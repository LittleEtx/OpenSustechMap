package com.littleetx.sustechnav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.layer.Anchor
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
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
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sustechnav.composeapp.generated.resources.Res
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds


const val CUSTOM_MAP_DATA_KEY = "custom-map-data"

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
    val editingModeState = remember { mutableStateOf(false) }
    val isDragEnable = remember { mutableStateOf(true) }
    val selectedNode = remember { mutableStateOf<MapNode?>(null) }

    LaunchedEffect(selectedNode) {
        if (selectedNode.value == null) {
            return@LaunchedEffect
        }
        cameraState.animateTo(
            finalPosition = cameraState.position.copy(
                target = Position(
                    longitude = selectedNode.value!!.lon,
                    latitude = selectedNode.value!!.lat,
                ),
                zoom = max(cameraState.position.zoom, 18.0)
            ),
            duration = 2.seconds,
        )
    }

    Box(modifier = modifier) {
        MaplibreMap(
            cameraState = cameraState,
            styleState = styleState,
            styleUri = Res.getUri("files/osm_liberty.json"),
            ornamentSettings = OrnamentSettings.AllDisabled,
            gestureSettings = GestureSettings(isScrollGesturesEnabled = isDragEnable.value),
            onMapClick = { pos, offset ->
                val feature = cameraState.queryRenderedFeatures(offset)
                    .firstOrNull { CUSTOM_MAP_DATA_KEY in it.properties }

                if (feature == null) {
                    selectedNode.value = null
                    return@MaplibreMap ClickResult.Pass
                }

                when (feature.properties[CUSTOM_MAP_DATA_KEY]) {
                    JsonPrimitive("node") -> {
                        val point = feature.geometry as Point
                        Logger.i { "Point: id = ${feature.id}, lat = ${point.coordinates.latitude}, lon = ${point.coordinates.longitude}" }
                        selectedNode.value = MapNode(
                            id = feature.id ?: "".also { Logger.w { "Null id for selected point feature!" } },
                            lat = point.coordinates.latitude,
                            lon = point.coordinates.longitude,
                        )
                    }
                    else -> return@MaplibreMap ClickResult.Pass
                }
                ClickResult.Consume
            }
        ) {

            val editorPoints = rememberGeoJsonSource(
                id = "editor-points-source",
                data = FeatureCollection()
            )

            LaunchedEffect(Unit) {
                val bytes = Res.readBytes("files/map_data.json")
                val mapData = Json.decodeFromString<MapData>(bytes.decodeToString())
                val nodeFeatures = mapData.nodes.map {
                    Feature(
                        id = it.id,
                        geometry = Point(
                            coordinates = Position(longitude = it.lon, latitude = it.lat)
                        ),
                        properties = mapOf(CUSTOM_MAP_DATA_KEY to JsonPrimitive("node"))
                    )
                }
                editorPoints.setData(FeatureCollection(nodeFeatures))
            }

            Anchor.Below("building-3d") {
                CircleLayer(
                    id = "editor-points",
                    source = editorPoints,
                    radius = const(5.dp),
                    color = const(Color.Black),
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            Button(
                onClick = { editingModeState.value = !editingModeState.value },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 20.dp),
            ) {
                Text(if (editingModeState.value) { "关闭编辑模式" } else { "开启编辑模式" })
            }

            val node = selectedNode.value
            if (node != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("节点 ID: ${node.id}", modifier = Modifier.padding(2.dp))
                        Text("经度：${node.lat}", modifier = Modifier.padding(2.dp))
                        Text("纬度：${node.lon}", modifier = Modifier.padding(2.dp))
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