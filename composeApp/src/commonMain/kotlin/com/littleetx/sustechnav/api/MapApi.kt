package com.littleetx.sustechnav.api

import com.littleetx.sustechnav.MapData
import com.littleetx.sustechnav.MapNode
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType


suspend fun ApiPayload.updateNode(newNode: MapNode): Result<Unit> {
    val response = client.post("/api/v1/node") {
        contentType(ContentType.Application.Json)
        setBody(newNode)
    }
    return response.makeResult()
}

suspend fun ApiPayload.getMapData(): Result<MapData> {
    val response = client.get("/api/v1/map")
    return response.makeResult()
}