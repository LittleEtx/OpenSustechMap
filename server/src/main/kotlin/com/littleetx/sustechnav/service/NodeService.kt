package com.littleetx.sustechnav.service

import com.littleetx.sustechnav.MapNode
import com.littleetx.sustechnav.database.MapNodeEntity
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.transactions.transaction

fun createOrUpdateNode(node: MapNode): MapNodeEntity {
    val id = try {
        node.id.toInt()
    } catch (e: NumberFormatException) {
        throw BadRequestException(message = "Node id must be integer", cause = e)
    }

    return transaction {
        MapNodeEntity.findByIdAndUpdate(id) {
            it.longitude = node.lon
            it.latitude = node.lat
        } ?: MapNodeEntity.new(id) {
            longitude = node.lon
            latitude = node.lat
        }
    }
}