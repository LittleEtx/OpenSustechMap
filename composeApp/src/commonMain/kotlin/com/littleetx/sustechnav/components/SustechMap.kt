package com.littleetx.sustechnav.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.littleetx.sustechnav.NodeId
import com.littleetx.sustechnav.data.NavigationData
import com.littleetx.sustechnav.data.getNode
import com.littleetx.sustechnav.data.notContainsNode
import dev.sargunv.maplibrecompose.compose.CameraState
import dev.sargunv.maplibrecompose.compose.ClickResult
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.layer.Anchor
import dev.sargunv.maplibrecompose.compose.layer.CircleLayer
import dev.sargunv.maplibrecompose.compose.layer.LineLayer
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.OrnamentSettings
import dev.sargunv.maplibrecompose.expressions.dsl.const
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
    cameraState: CameraState,
    mode: MapMode,
    navData: NavigationData,
    modifier: Modifier = Modifier,
    isEditing: Boolean,
    selectedFeature: SelectedFeature?,
    setSelectedFeature: (SelectedFeature?) -> Unit,
    selectedNodeNewPosition: Position,
) {
    val styleState = rememberStyleState()

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

    MaplibreMap(
        modifier = modifier,
        cameraState = cameraState,
        styleState = styleState,
        styleUri = Res.getUri("files/osm_liberty.json"),
        ornamentSettings = OrnamentSettings.AllDisabled,
        onMapClick = { pos, offset ->
            if (isEditing) {
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
                setSelectedFeature(null)
                return@MaplibreMap ClickResult.Pass
            }

            when (feature.properties[FEATURE_TYPE]) {
                nodeType -> {
                    val point = feature.geometry as Point
                    Logger.i { "Point: id = ${feature.id}, lat = ${point.coordinates.latitude}, lon = ${point.coordinates.longitude}" }
                    setSelectedFeature(
                        Json.decodeFromJsonElement<SelectedNode>(feature.properties[FEATURE_VALUE]!!)
                    )
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
            data = if (isEditing) {
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
                visible = isEditing,
                source = editingPointLine,
                width = const(5.dp),
                color = const(Color.Green),
                dasharray = const(listOf(2, 1))
            )

            CircleLayer(
                id = "editing-point-green",
                visible = isEditing,
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
}