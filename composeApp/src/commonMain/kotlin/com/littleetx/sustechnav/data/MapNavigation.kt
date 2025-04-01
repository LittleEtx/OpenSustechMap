package com.littleetx.sustechnav.data

import com.littleetx.sustechnav.ConnectionType
import com.littleetx.sustechnav.MapData
import com.littleetx.sustechnav.NodeId
import io.github.dellisd.spatialk.geojson.Position


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
    )
}
