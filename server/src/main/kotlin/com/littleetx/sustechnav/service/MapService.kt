package com.littleetx.sustechnav.service

import com.littleetx.sustechnav.*
import com.littleetx.sustechnav.database.*
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.LineString
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import io.ktor.server.plugins.*
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * 获得当前所有的地图数据
 */
fun getMapData(): MapData {
    return transaction {
        val nodes = MapNodeEntity.all().map {
            MapNode(
                id = it.id.value.toString(),
                lat = it.latitude,
                lon = it.longitude,
            )
        }.toList()

        val connections = ConnectionEntity.all().map {
            Connection(
                node1 = it.node1.id.value.toString(),
                node2 = it.node2.id.value.toString(),
                directional = it.directional,
                type = it.type,
            )
        }.toList()

        val pois = PoiEntity.all().map {
            Poi(
                id = it.id.value.toString(),
                node = it.node.id.value.toString(),
                name = it.name.toString(),
                tags = it.tags.associate { entity -> entity.key.value to entity.value }
            )
        }

        MapData(
            nodes = nodes,
            connections = connections,
            pois = pois,
        )
    }
}

/**
 * 清除所有地图数据
 */
fun resetMapData() {
    transaction {
        ConnectionTable.deleteAll()
        MapNodeTable.deleteAll()
    }
}


fun checkDatabaseCanInit() {
    transaction {
        if (MapNodeEntity.count() != 0L || ConnectionEntity.count() != 0L) {
            throw BadRequestException(message = "Database is not empty! Please use the delete api to reset database before initialize")
        }
    }
}

private data class InternalConnection(
    val node1: Int,
    val node2: Int,
    val type: ConnectionType,
    val directional: Boolean = false,
)

private data class InternalPoi(
    val id: Int,
    val node: Int,
    val name: String,
    val tags: Map<String, String> = mutableMapOf()
)

val logger = LoggerFactory.getLogger("MapServive")

/**
 * 从Geojson初始化地图数据
 */
fun initFromGeojson(features: FeatureCollection) {
    checkDatabaseCanInit()

    var currentNodeId = 0
    var currentPoiId = 0
    val positions = mutableMapOf<Position, Int>()
    val connections = mutableListOf<InternalConnection>()
    val pois = mutableListOf<InternalPoi>()

    for (feature in features) {
        val geometry = feature.geometry
        when (geometry) {
            is LineString -> {
                if ("highway" in feature.properties) {
                    geometry.coordinates.forEachIndexed { idx, pos ->
                        val id = positions.getOrPut(pos) { currentNodeId++ }
                        if (idx > 0) {
                            val last = geometry.coordinates[idx - 1]
                            val lastId = positions[last]!!

                            connections += InternalConnection(node1 = lastId, node2 = id, type = ConnectionType.Walk)
                        }
                    }
                }
            }
            is Point -> {
                if ("name" in feature.properties) {
                    val nodeId = positions.getOrPut(geometry.coordinates) { currentNodeId++ }
                    try {
                        val name = feature.getProperty<JsonPrimitive>("name")!!
                        val otherTags = feature.getProperty<JsonPrimitive>("other_tags")?.let {
                            it.content.split(",").associate {
                                val (seg1, seg2) = it.split("=>")
                                seg1.trim { c -> c.isWhitespace() || c == '"' } to seg2.trim { c -> c.isWhitespace() || c == '"' }
                            }
                        } ?: emptyMap()
                        pois += InternalPoi(id = currentPoiId++, node = nodeId, name = name.content, tags = otherTags)
                    } catch (e: Exception) {
                        logger.error("Failed to parse poi: $feature", e)
                    }
                }
            }
            else -> {
                continue
            }
        }
    }

    transaction {
        MapNodeTable.batchInsert(positions.entries) { (pos, id) ->
            this[MapNodeTable.id] = id
            this[MapNodeTable.latitude] = pos.latitude
            this[MapNodeTable.longitude] = pos.longitude
        }

        ConnectionTable.batchInsert(connections) { conn ->
            this[ConnectionTable.node1] = conn.node1
            this[ConnectionTable.node2] = conn.node2
            this[ConnectionTable.directional] = conn.directional
            this[ConnectionTable.type] = conn.type
        }

        PoiTable.batchInsert(pois) { poi ->
            this[PoiTable.node] = poi.node
            this[PoiTable.name] = poi.name
        }

        PoiTagTable.batchInsert(pois.flatMap { poi ->
            poi.tags.map { (key, value) ->
                Triple(poi.id, key, value)
            }
        }) { (id, key, value) ->
            this[PoiTagTable.poi] = id
            this[PoiTagTable.key] = key
            this[PoiTagTable.value] = value
        }
    }

}