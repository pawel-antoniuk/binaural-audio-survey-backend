package pl.antoniuk

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.CachingOptions
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.Level
import java.io.File

object Config {
    val audioDir = System.getenv("AUDIO_FILES_DIR")
        ?: error("AUDIO_FILES_DIR environment variable is not set")
    val recaptchaSecret = System.getenv("RECAPTCHA_SECRET_KEY")
        ?: error("RECAPTCHA_SECRET_KEY environment variable is not set")
    val numberOfQuestions: Int = System.getenv("AUDIO_FILES_LIMIT")?.toInt()
        ?: error("AUDIO_FILES_LIMIT environment variable is not set")
}

val httpClient = HttpClient(CIO) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        json(customJson)
    }
}

fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
        filter { _ -> true } // Log all requests
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.uri
            val headers = call.request.headers.entries()
                .joinToString("\n") { "${it.key}: ${it.value.joinToString()}" }
            val queryParams = call.request.queryParameters.entries()
                .joinToString("\n") { "${it.key}: ${it.value.joinToString()}" }

            buildString {
                appendLine("Request:")
                appendLine("Path: $path")
                appendLine("Method: $httpMethod")
                appendLine("Status: $status")
                appendLine("Headers:")
                appendLine(headers)
                appendLine("Query Parameters:")
                appendLine(queryParams)
            }
        }
    }
    install(ContentNegotiation) { json() }
    install(CORS) {
        val frontendAddr = System.getenv("FRONTEND_ADDR")
            ?: error("FRONTEND_ADDR environment variable is not set")
        allowHost(frontendAddr)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HEADER_RECAPTCHA_TOKEN)
        allowCredentials = true
    }

    routing {

        route("/api") {
            route("/v1") {
                staticFiles("/audio", File(Config.audioDir)) {
                    cacheControl {
                        listOf(Immutable, CacheControl.MaxAge(10000))
                    }
                }
                questions()
                user()
                answers()
                comments()
                messages()
            }
        }
    }
}

suspend fun verifyCaptcha(token: String): RecaptchaResponse {
    return runCatching {
        httpClient.post("https://www.google.com/recaptcha/api/siteverify") {
            parameter("secret", Config.recaptchaSecret)
            parameter("response", token)
        }.body<RecaptchaResponse>()
    }.getOrElse {
        RecaptchaResponse(success = false, `error-codes` = listOf("request-failed"))
    }
}

suspend fun handleCaptchaError(result: RecaptchaResponse, call: ApplicationCall) {
    val errorMessage = when (result.`error-codes`?.firstOrNull()) {
        "missing-input-secret" -> "The secret key is missing"
        "invalid-input-secret" -> "The secret key is invalid"
        "missing-input-response" -> "The response parameter is missing"
        "invalid-input-response" -> "The response parameter is invalid"
        "bad-request" -> "The request was rejected because it was malformed"
        "timeout-or-duplicate" -> "The response is no longer valid"
        else -> "Captcha verification failed"
    }
    call.respond(HttpStatusCode.Unauthorized, errorMessage)
}

fun main() {
    Database.init()
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

object Immutable : CacheControl(null) {
    override fun toString(): String = "immutable"
}