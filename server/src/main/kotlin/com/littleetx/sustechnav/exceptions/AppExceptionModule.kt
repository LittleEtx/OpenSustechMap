package com.littleetx.sustechnav.exceptions

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: Int,
    val message: String,
    val details: Map<String, String> = emptyMap()
)

// 2. 定义自定义异常类
sealed class AppException(
    val httpStatus: HttpStatusCode,
    override val message: String,
    val details: Map<String, String> = emptyMap()
) : RuntimeException(message)


class InternalException(
    message: String = "Internal server error",
    details: Map<String, String> = emptyMap()
) : AppException(HttpStatusCode.InternalServerError, message, details)

class BadRequestException(
    message: String = "Invalid request",
    details: Map<String, String> = emptyMap()
) : AppException(HttpStatusCode.BadRequest, message, details)

class NotFoundException(
    message: String = "Resource not found",
    details: Map<String, String> = emptyMap()
) : AppException(HttpStatusCode.NotFound, message, details)

// 3. 修改Application.module()配置
fun Application.module() {
    // 安装StatusPages插件
    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(
                cause.httpStatus,
                ErrorResponse(
                    code = cause.httpStatus.value,
                    message = cause.message,
                    details = cause.details
                )
            )
        }

        exception<Throwable> { call, cause ->
            val status = when (cause) {
                is IllegalArgumentException -> HttpStatusCode.BadRequest
                else -> HttpStatusCode.InternalServerError
            }

            call.respond(
                status,
                ErrorResponse(
                    code = status.value,
                    message = cause.message ?: "Internal server error",
                    details = mapOf("type" to (cause::class.simpleName ?: "Unknown"))
                )
            )

            this@module.log.error("Unhandled exception", cause)
        }
    }
}