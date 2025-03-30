package com.littleetx.sustechnav

import kotlinx.serialization.Serializable

typealias NodeId = String

@Serializable
data class MapNode(
    val id: NodeId,
    val lat: Double,
    val lon: Double,
)


@Serializable
class Connection(
    val nodes: Array<NodeId>,
    val name: String? = null,
    val types: Array<String>,
)

@Serializable
class MapData(
    val nodes: Array<MapNode>,
    val connections: Array<Connection>,
)