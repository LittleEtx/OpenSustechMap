package com.littleetx.sustechnav.routes

import com.littleetx.sustechnav.MapNode
import com.littleetx.sustechnav.service.createOrUpdateNode
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.nodeRoute() {
    post {
        val node = call.receive<MapNode>()
        createOrUpdateNode(node)
        call.response.status(HttpStatusCode.OK)
    }
}