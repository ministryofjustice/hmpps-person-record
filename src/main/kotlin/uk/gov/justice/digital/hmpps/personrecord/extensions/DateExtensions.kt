package uk.gov.justice.digital.hmpps.personrecord.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val UK_ZONE: ZoneId = ZoneId.of("Europe/London")

val zonedDateTimeComparator: Comparator<ZonedDateTime> = { a, b -> a.toInstant().compareTo(b.toInstant()) }

// Instant extensions
fun Instant.asStringWithUkZone(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(UK_ZONE).format(this)

fun LocalDate.toLocalDateTime(): LocalDateTime = this.atStartOfDay()
