package pl.antoniuk

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

abstract class TimestampedTable(val name: String) : UUIDTable(name) {
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now(Clock.systemUTC()) }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now(Clock.systemUTC()) }
}

object Users : TimestampedTable("users") {
    val age = varchar("age", 10)
    val hearingDifficulties = bool("hearing_difficulties")
    val listeningTestParticipation = bool("listening_test_participation")
    val headphonesMakeAndModel = text("headphones_make_model")
    val customIdentifier = text("identifier").nullable()
    val metadata = text("metadata")
}

object Questions : TimestampedTable("questions") {
    val audioFilename = varchar("audio_filename", 255)
}

object Answers : TimestampedTable("answers") {
    val userId = reference("user_id", Users)
    val questionId = reference("question_id", Questions)
    val audioFilename = varchar("audio_filename", 255)
    val leftAngle = float("left_angle")
    val rightAngle = float("right_angle")
    val ensembleWidth = float("ensemble_width")
}

object Comments : TimestampedTable("comments") {
    val userId = reference("user_id", Users)
    val questionId = reference("question_id", Questions)
    val message = text("message")
}

object Messages : TimestampedTable("messages") {
    val userId = reference("user_id", Users)
    val content = text("message")
}

fun updateTimestamp(table: TimestampedTable, entityId: UUID) {
    table.update({ table.id eq entityId }) {
        it[table.updatedAt] = LocalDateTime.now(Clock.systemUTC())
    }
}

fun UserEntity.Companion.upsert(id: UUID, block: UserEntity.() -> Unit): UserEntity {
    return transaction {
        find { Users.id eq id }.firstOrNull()?.apply {
            updateTimestamp(Users, id)
            block()
        } ?: new(id) {
            block()
        }
    }
}