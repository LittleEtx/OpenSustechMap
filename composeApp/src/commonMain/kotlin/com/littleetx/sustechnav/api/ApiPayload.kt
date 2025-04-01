package com.littleetx.sustechnav.api

import co.touchlab.kermit.Logger
import com.littleetx.sustechnav.ErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

data class ApiPayload(
    val host: String = "10.0.2.2:8080",
    val client: HttpClient = HttpClient {
        defaultRequest {
           url {
               this.protocol = URLProtocol.HTTP
               this.host = host
           }

        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    Logger.withTag("KtorHttpClient").v { message }
                }
            }
        }
    }
)

sealed class Result<T> {
    abstract val code: HttpStatusCode
}

class Success<T>(
    override val code: HttpStatusCode = HttpStatusCode.OK,
    val value: T,
) : Result<T>()

class Failure<T>(
    override val code: HttpStatusCode,
    val message: String,
    val details: Map<String, String> = emptyMap(),
) : Result<T>() {
    constructor(response: ErrorResponse) : this(
        code = HttpStatusCode.fromValue(response.code),
        message = response.message,
        details = response.details,
    )
}

suspend inline fun<reified T> HttpResponse.makeResult(): Result<T> {
    return try {
        if (status.value in 200..299) {
            Success(
                code = status,
                value = body(),
            )
        } else {
            val errorResponse: ErrorResponse = body()
            Failure(
                code = status,
                message = errorResponse.message,
                details = errorResponse.details,
            )
        }
    } catch (e: NoTransformationFoundException) {
        Logger.e(e) { "Failed to recognize response type" }
        Failure(
            code = status,
            message = "Failed to recognize response type",
        )
    }
}