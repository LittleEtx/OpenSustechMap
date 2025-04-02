package com.littleetx.sustechnav.data

import com.littleetx.sustechnav.ConnectionType
import com.littleetx.sustechnav.MapData
import com.littleetx.sustechnav.NodeId
import com.littleetx.sustechnav.Poi
import io.github.dellisd.spatialk.geojson.Position
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


data class NodeConnection(
    val origin: NodeId,
    val target: NodeId,
    val type: ConnectionType,
)

data class NodeInfo(
    val id: NodeId,
    val position: Position,
)

data class NavigationData(
    val nodes: Map<NodeId, NodeInfo> = emptyMap(),
    val nodeConn: Map<NodeId, List<NodeConnection>> = emptyMap(),
    val pois: List<Poi> = emptyList(),
)

fun NavigationData.containsNode(id: NodeId) = id in nodes
fun NavigationData.notContainsNode(id: NodeId) = id !in nodes

fun NavigationData.getNode(id: NodeId) = nodes[id]

fun MapData.toNavigationData(): NavigationData {
    val nodes = nodes.associate {
        it.id to NodeInfo(
            id = it.id,
            position = Position(
                longitude = it.lon,
                latitude = it.lat,
            )
        )
    }
    val nodeConn = mutableMapOf<NodeId, MutableList<NodeConnection>>()
    fun processConn(origin: NodeId, target: NodeId, type: ConnectionType) {
        val connSet = nodeConn.getOrPut(origin) { mutableListOf() }
        connSet.add(
            NodeConnection(
                origin = origin,
                target = target,
                type = type,
            )
        )
    }

    connections.forEach {
        processConn(it.node1, it.node2, it.type)
        if (!it.directional) {
            processConn(it.node2, it.node1, it.type)
        }
    }

    return NavigationData(
        nodes = nodes,
        nodeConn = nodeConn,
        pois = pois,
    )
}

infix fun Position.distanceTo(other: Position): Double {
    val earthRadius = 6371000.0 // Earth radius in meters

    val lat1 = this.latitude.toRadians()
    val lon1 = this.longitude.toRadians()
    val lat2 = other.latitude.toRadians()
    val lon2 = other.longitude.toRadians()

    val dLat = lat2 - lat1
    val dLon = lon2 - lon1

    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

private fun Double.toRadians() = this * PI / 180