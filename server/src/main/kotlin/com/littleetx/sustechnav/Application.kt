package com.littleetx.sustechnav

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }

        route("/mapdata") {
            get {

            }

            post("/node") {

            }
        }
    }
}