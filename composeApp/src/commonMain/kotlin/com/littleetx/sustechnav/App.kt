package com.littleetx.sustechnav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.OrnamentSettings
import dev.sargunv.maplibrecompose.material3.controls.AttributionButton
import dev.sargunv.maplibrecompose.material3.controls.DisappearingCompassButton
import dev.sargunv.maplibrecompose.material3.controls.DisappearingScaleBar
import dev.sargunv.maplibrecompose.material3.controls.ScaleBarMeasure
import dev.sargunv.maplibrecompose.material3.controls.ScaleBarMeasures
import io.github.dellisd.spatialk.geojson.Position
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview
import sustechnav.composeapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(latitude = 22.602, longitude = 113.995),
            zoom = 14.5,
        )
    )

    val styleState = rememberStyleState()

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MaplibreMap(
                cameraState = cameraState,
                styleState = styleState,
                styleUri = Res.getUri("files/osm_liberty.json"),
                ornamentSettings = OrnamentSettings.AllDisabled,
            )

            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
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
}