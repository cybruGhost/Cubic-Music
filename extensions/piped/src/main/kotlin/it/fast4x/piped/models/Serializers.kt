package it.fast4x.piped.models

import io.ktor.http.Url
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UrlSerializer : KSerializer<Url> {
    override val descriptor = PrimitiveSerialDescriptor("Url", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = Url(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: Url) = encoder.encodeString(value.toString())
}

typealias UrlString = @Serializable(with = UrlSerializer::class) Url

object SecondLocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("DateTimeSeconds", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder) =
        Instant.fromEpochSeconds(decoder.decodeLong()).toLocalDateTime(TimeZone.UTC)

    override fun serialize(encoder: Encoder, value: LocalDateTime) =
        encoder.encodeLong(value.toInstant(TimeZone.UTC).epochSeconds)
}

typealias DateTimeSeconds = @Serializable(with = SecondLocalDateTimeSerializer::class) LocalDateTime

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
}

typealias UUIDString = @Serializable(with = UUIDSerializer::class) UUID
