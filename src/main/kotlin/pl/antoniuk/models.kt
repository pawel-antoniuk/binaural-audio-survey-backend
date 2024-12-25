package pl.antoniuk

import kotlinx.serialization.Serializable

const val HEADER_RECAPTCHA_TOKEN = "X-Recaptcha-Token"

@Serializable
data class Question(val id: String, val audioFilename: String)

@Serializable
data class ApiResponse<T>(val data: T, val message: String? = null, val success: Boolean = true)

@Serializable
data class RecaptchaResponse(
    val success: Boolean,
    val score: Double? = null,
    val action: String? = null,
    val challenge_ts: String? = null,
    val hostname: String? = null,
    val `error-codes`: List<String>? = null
)

@Serializable
data class Comment(val userId: String, val questionId: String, val message: String)

@Serializable
data class Message(val userId: String, val content: String)

@Serializable
data class Answer(
    val userId: String,
    val questionId: String,
    val audioFilename: String,
    val leftAngle: Float,
    val rightAngle: Float,
    val ensembleWidth: Float
)

@Serializable
data class User(val id: String, val questionnaire: Questionnaire, val metadata: String) {
    @Serializable
    data class Questionnaire(
        val age: String,
        val hearingDifficulties: Boolean,
        val listeningTestParticipation: Boolean,
        val headphonesMakeAndModel: String = "",
        val identifier: String? = null
    )
}