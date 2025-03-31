package com.littleetx.sustechnav.database

import com.littleetx.sustechnav.ConnectionType
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object ConnectionTable : IntIdTable("t_connection") {
    val node1 = reference("node1", MapNodeTable)
    val node2 = reference("node2", MapNodeTable)
    val type = enumeration<ConnectionType>("type")
    val directional = bool("directional").default(false)

    init {
        uniqueIndex(node1, node2, type, directional)
    }
}

class ConnectionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ConnectionEntity>(ConnectionTable)

    var node1 by MapNodeEntity referencedOn ConnectionTable.node1
    var node2 by MapNodeEntity referencedOn ConnectionTable.node2
    var type by ConnectionTable.type
    var directional by ConnectionTable.directional
}