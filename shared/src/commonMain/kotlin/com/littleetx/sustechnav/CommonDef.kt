package com.littleetx.sustechnav

import kotlinx.serialization.Serializable

enum class ConnectionType {
    Walk, Bike, Car, Bus
}

typealias NodeId = String

/**
 * 节点，可以用作连接图的节点，也可以用作多边形的端点
 * @property id 节点唯一id
 * @property lat 纬度
 * @property lon 经度
 */
@Serializable
data class MapNode(
    val id: NodeId,
    val lat: Double,
    val lon: Double,
)

/**
 * 表示连接图中的一条边
 * @property node1 起点id
 * @property node2 终点id
 * @property directional 是否是有向边，默认无向
 * @property type 边的可通行种类
 */
@Serializable
class Connection(
    val node1: NodeId,
    val node2: NodeId,
    val directional: Boolean = false,
    val type: ConnectionType,
)


typealias PoiId = String

/**
 * 表示图上的一个兴趣点
 * @property id 兴趣点id
 * @property node 节点id
 * @property name 兴趣点名称
 * @property tags 兴趣点标签
 */
@Serializable
class Poi(
    val id: PoiId,
    val node: NodeId,
    val name: String,
    val tags: Map<String, String>,
)

@Serializable
class MapData(
    val nodes: List<MapNode>,
    val connections: List<Connection>,
    val pois: List<Poi>,
)

/**
 * 当应答不是200时，都会返回这个结构
 */
@Serializable
data class ErrorResponse(
    val code: Int,
    val message: String,
    val details: Map<String, String> = emptyMap()
)
