package com.littleetx.sustechnav.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object MapNodeTable : IntIdTable("t_mapnode") {
    val longitude = double("longitude")
    val latitude = double("latitude")
}

class MapNodeEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MapNodeEntity>(MapNodeTable)

    var longitude by MapNodeTable.longitude
    var latitude by MapNodeTable.latitude
}

