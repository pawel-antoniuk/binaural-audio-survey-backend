package pl.antoniuk

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(Users)

    var age by Users.age
    var hearingDifficulties by Users.hearingDifficulties
    var listeningTestParticipation by Users.listeningTestParticipation
    var headphonesMakeAndModel by Users.headphonesMakeAndModel
    var customIdentifier by Users.customIdentifier
    var metadata by Users.metadata
}

class QuestionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<QuestionEntity>(Questions)

    var audioFilename by Questions.audioFilename

    fun toQuestion() = Question(
        id = id.value.toString(),
        audioFilename = audioFilename
    )
}

class AnswerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AnswerEntity>(Answers)

    var user by UserEntity referencedOn Answers.userId
    var question by QuestionEntity referencedOn Answers.questionId
    var audioFilename by Answers.audioFilename
    var leftAngle by Answers.leftAngle
    var rightAngle by Answers.rightAngle
    var ensembleWidth by Answers.ensembleWidth
}

class CommentEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CommentEntity>(Comments)

    var user by UserEntity referencedOn Comments.userId
    var question by QuestionEntity referencedOn Comments.questionId
    var message by Comments.message
}

class MessageEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MessageEntity>(Messages)

    var user by UserEntity referencedOn Messages.userId
    var content by Messages.content
}