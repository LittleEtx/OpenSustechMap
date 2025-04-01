package com.littleetx.sustechnav.plugins

import com.littleetx.sustechnav.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*


// 3. 修改Application.module()配置
fun Application.module() {
    // 安装StatusPages插件
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            val status = HttpStatusCode.BadRequest
            call.respond(
                status,
                ErrorResponse(
                    code = status.value,
                    message = cause.message ?: "Internal server error",
                    details = mapOf("type" to (cause::class.simpleName ?: "Unknown")),
                )
            )
        }

        exception<NotFoundException> { call, cause ->
            val status = HttpStatusCode.NotFound
            call.respond(
                status,
                ErrorResponse(
                    code = status.value,
                    message = cause.message ?: "Resources not found",
                    details = mapOf("type" to (cause::class.simpleName ?: "Unknown")),
                )
            )
        }

        exception<ContentTransformationException> { call, cause ->
            val status = HttpStatusCode.BadRequest
            call.respond(
                status,
                ErrorResponse(
                    code = status.value,
                    message = cause.message ?: "Not supported content",
                    details = mapOf("type" to (cause::class.simpleName ?: "Unknown")),
                )
            )
        }

        exception<Throwable> { call, cause ->
            // Unhandled error
            val status = HttpStatusCode.InternalServerError
            call.respond(
                status,
                ErrorResponse(
                    code = status.value,
                    message = cause.message ?: "Internal server error",
                    details = mapOf("type" to (cause::class.simpleName ?: "Unknown")),
                )
            )
            this@module.log.error("Unhandled exception", cause)
        }
    }
}