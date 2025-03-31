package com.littleetx.sustechnav.service

import com.littleetx.sustechnav.Connection
import com.littleetx.sustechnav.ConnectionType
import com.littleetx.sustechnav.MapData
import com.littleetx.sustechnav.MapNode
import com.littleetx.sustechnav.database.ConnectionEntity
import com.littleetx.sustechnav.database.ConnectionTable
import com.littleetx.sustechnav.database.MapNodeEntity
import com.littleetx.sustechnav.database.MapNodeTable
import com.littleetx.sustechnav.exceptions.BadRequestException
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.LineString
import io.github.dellisd.spatialk.geojson.Position
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * 获得当前所有的地图数据
 */
fun getMapData(): MapData {
    return transaction {
        val nodes = MapNodeEntity.all().map {
            MapNode(
                id = it.id.toString(),
                lat = it.latitude,
                lon = it.longitude,
            )
        }.toList()

        val connections = ConnectionEntity.all().map {
            Connection(
                node1 = it.node1.id.toString(),
                node2 = it.node2.id.toString(),
                directional = it.directional,
                type = it.type,
            )
        }.toList()

        MapData(
            nodes = nodes,
            connections = connections,
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
    if (MapNodeEntity.count() != 0L || ConnectionEntity.count() != 0L) {
        throw BadRequestException(message = "Database is not empty! Please use the delete api to reset database before initialize")
    }
}

data class InternalConnection(
    val node1: Int,
    val node2: Int,
    val type: ConnectionType,
    val directional: Boolean = false,
)

/**
 * 从Geojson初始化地图数据
 */
fun initFromGeojson(features: FeatureCollection) {
    checkDatabaseCanInit()

    var currentId = 0
    val positions = mutableMapOf<Position, Int>()
    val connections = mutableListOf<InternalConnection>()

    for (feature in features) {
        val geometry = feature.geometry
        when (geometry) {
            is LineString -> {
                if (feature.properties.contains("highway")) {
                    geometry.coordinates.forEachIndexed { idx, pos ->
                        val id = positions.getOrPut(pos) { currentId++ }
                        if (idx > 0) {
                            val last = geometry.coordinates[idx - 1]
                            val lastId = positions[last]!!

                            connections += InternalConnection(node1 = lastId, node2 = id, type = ConnectionType.Walk)
                        }
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
    }

}