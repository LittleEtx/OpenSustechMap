package com.littleetx.sustechnav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.OrnamentSettings
import dev.sargunv.maplibrecompose.expressions.dsl.const
import dev.sargunv.maplibrecompose.material3.controls.AttributionButton
import dev.sargunv.maplibrecompose.material3.controls.DisappearingCompassButton
import dev.sargunv.maplibrecompose.material3.controls.DisappearingScaleBar
import dev.sargunv.maplibrecompose.material3.controls.ScaleBarMeasure
import dev.sargunv.maplibrecompose.material3.controls.ScaleBarMeasures
import io.github.dellisd.spatialk.geojson.Feature
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sustechnav.composeapp.generated.resources.Res


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
    val editingModeState = remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        MaplibreMap(
            cameraState = cameraState,
            styleState = styleState,
            styleUri = Res.getUri("files/osm_liberty.json"),
            ornamentSettings = OrnamentSettings.AllDisabled,
        ) {

            val editorPoints = rememberGeoJsonSource(
                id = "editor-points",
                data = FeatureCollection()
            )

            LaunchedEffect(Unit) {
                val bytes = Res.readBytes("files/map_data.json")
                val mapData = Json.decodeFromString<MapData>(bytes.decodeToString())
                val nodeFeatures = mapData.nodes.map {
                    Feature(geometry = Point(coordinates = Position(longitude = it.lon, latitude = it.lat)))
                }
                editorPoints.setData(FeatureCollection(nodeFeatures))
            }


            CircleLayer(
                id = "editor-points",
                source = editorPoints,
                radius = const(5.dp),
                color = const(Color.Black),
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            Button(
                onClick = { editingModeState.value = !editingModeState.value },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 20.dp),
            ) {
                Text(if (editingModeState.value) { "关闭编辑模式" } else { "开启编辑模式" })
            }

            DisappearingScaleBar(
                measures = ScaleBarMeasures(primary = ScaleBarMeasure.Metric),
                metersPerDp = cameraState.metersPerDpAtTarget,
                zoom = cameraState.position.zoom,
                modifier = Modifier.align(Alignment.TopStart),
            )
            DisappearingCompassButton(cameraState, modifier = Modifier.align(Alignment.TopEnd))
            AttributionButton(styleState, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}