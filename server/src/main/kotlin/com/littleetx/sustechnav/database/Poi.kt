package com.littleetx.sustechnav.database

import org.jetbrains.exposed.dao.CompositeEntity
import org.jetbrains.exposed.dao.CompositeEntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object PoiTable : IntIdTable("t_poi") {
    val node = reference("node", MapNodeTable)
    val name = varchar("name", 128)
}

object PoiTagTable : CompositeIdTable("t_poi_tag") {
    val poi = reference("poi", PoiTable)
    val key = varchar("key", 32).entityId()
    val value = varchar("value", 256)

    init {
        addIdColumn(poi)
    }

    override val primaryKey = PrimaryKey(poi, key)
}

class PoiEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PoiEntity>(PoiTable)

    var node by MapNodeEntity referencedOn PoiTable.node
    var name by PoiTable.name
    val tags by PoiTagEntity referrersOn PoiTagTable.poi
}

class PoiTagEntity(id : EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<PoiTagEntity>(PoiTagTable)

    val key by PoiTagTable.key
    var value by PoiTagTable.value
}