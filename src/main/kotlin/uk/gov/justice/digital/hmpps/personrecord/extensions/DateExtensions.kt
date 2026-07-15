package uk.gov.justice.digital.hmpps.personrecord.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

private val UK_ZONE: ZoneId = ZoneId.of("Europe/London")

fun Instant.asStringWithUkZone(): String = ISO_OFFSET_DATE_TIME.withZone(UK_ZONE).format(this)

fun LocalDateTime.asStringWithUkZone(): String = ISO_OFFSET_DATE_TIME.withZone(UK_ZONE).format(this.atZone(UK_ZONE))

fun LocalDate.toLocalDateTime(): LocalDateTime = this.atStartOfDay()
fun LocalDateTime.toZonedDateTime(): ZonedDateTime = this.atZone(UK_ZONE)
fun ZonedDateTime.toLocalDateTimeUk(): LocalDateTime = this.withZoneSameInstant(UK_ZONE).toLocalDateTime()
