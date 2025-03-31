package com.littleetx.sustechnav.routes

import com.littleetx.sustechnav.exceptions.BadRequestException
import com.littleetx.sustechnav.service.getMapData
import com.littleetx.sustechnav.service.initFromGeojson
import com.littleetx.sustechnav.service.resetMapData
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.mapRoute() {
    get {
        call.respond(HttpStatusCode.OK, getMapData())
    }

    delete {
        resetMapData()
        call.response.status(HttpStatusCode.OK)
    }

    post {
        val type = call.parameters["type"] ?: "geojson"

        when (type) {
            "geojson" -> {
                val geoJson = call.receiveText()
                val featureCollection = try {
                    FeatureCollection.fromJson(geoJson)
                } catch (e: Exception) {
                    throw BadRequestException(
                        message = "Invalid GeoJSON format",
                        details = mapOf("error" to (e.message ?: "Unknown parsing error"))
                    )
                }
                initFromGeojson(featureCollection)
            }
            else -> throw BadRequestException(message = "Unknown MapData type")
        }
        call.response.status(HttpStatusCode.OK)
    }


}