package pl.antoniuk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.UUID

fun Route.questions() {
    post("/questions") {
        withCaptchaVerification {
            val questions = getQuestions()
            call.respond(ApiResponse(questions))
        }
    }
}

fun Route.user() {
    post("/users") {
        withCaptchaVerification {
            val user = call.receive<User>()
            transaction {
                UserEntity.upsert(UUID.fromString(user.id)) {
                    age = user.questionnaire.age
                    hearingDifficulties = user.questionnaire.hearingDifficulties
                    listeningTestParticipation = user.questionnaire.listeningTestParticipation
                    headphonesMakeAndModel = user.questionnaire.headphonesMakeAndModel
                    customIdentifier = user.questionnaire.customIdentifier
                    metadata = refineMetadata(user.metadata)
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun Route.comments() {
    post("/comments") {
        withCaptchaVerification {
            val comment = call.receive<Comment>()
            transaction {
                CommentEntity.new {
                    user = UserEntity[UUID.fromString(comment.userId)]
                    question = QuestionEntity[UUID.fromString(comment.questionId)]
                    message = comment.message
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun Route.messages() {
    post("/messages") {
        withCaptchaVerification {
            val message = call.receive<Message>()
            transaction {
                MessageEntity.new {
                    user = UserEntity[UUID.fromString(message.userId)]
                    content = message.content
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun Route.answers() {
    post("/answers") {
        withCaptchaVerification {
            val answer = call.receive<Answer>()
            transaction {
                AnswerEntity.new {
                    user = UserEntity[UUID.fromString(answer.userId)]
                    question = QuestionEntity[UUID.fromString(answer.questionId)]
                    audioFilename = answer.audioFilename
                    leftAngle = answer.leftAngle
                    rightAngle = answer.rightAngle
                    ensembleWidth = answer.ensembleWidth
                }
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun RoutingContext.refineMetadata(metadata: String) =
    Json.parseToJsonElement(metadata)
        .jsonObject
        .toMutableMap()
        .apply {
            val serverMeta = buildJsonObject {
                put("client_ip", call.getClientIp())
            }
            put("server", serverMeta)
        }
        .let { JsonObject(it) }
        .toString()

fun ApplicationCall.getClientIp(): String {
    val xRealIp = request.headers["X-Real-IP"]
    if (!xRealIp.isNullOrEmpty()) {
        return xRealIp
    }

    val forwardedFor = request.headers["X-Forwarded-For"]
    if (!forwardedFor.isNullOrEmpty()) {
        return forwardedFor.split(",")[0].trim()
    }

    return request.local.remoteHost
}

suspend fun RoutingContext.withCaptchaVerification(
    block: suspend RoutingContext.() -> Unit
) {
    runCatching {
        val token = call.request.headers[HEADER_RECAPTCHA_TOKEN]
            ?: error("Recaptcha token is missing")
        val captchaResult = verifyCaptcha(token)

        when {
            !captchaResult.success -> handleCaptchaError(captchaResult, call)
            captchaResult.score?.let { it < 0.5 } == true -> {
                call.respond(HttpStatusCode.Unauthorized, "Suspicious activity detected")
            }
            else -> block()
        }
    }.onFailure {
        call.respond(HttpStatusCode.BadRequest, "Invalid request: ${it.message}")
    }
}

private fun getQuestions() = transaction {
    File(Config.audioDir)
        .walk()
        .filter { it.isFile }
        .shuffled()
        .take(Config.numberOfQuestions)
        .flatMap { listOf(it, it) }
        .shuffled()
        .map { file ->
            QuestionEntity.new {
                audioFilename = file.name
            }.toQuestion()
        }
        .toList()
}
