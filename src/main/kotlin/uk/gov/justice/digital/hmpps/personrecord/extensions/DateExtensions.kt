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

// LocalDate extensions
fun LocalDate.toUkZonedDateTime(): ZonedDateTime = this.atStartOfDay(UK_ZONE)

// ZonedDateTime extensions
fun ZonedDateTime.toUkLocalDate(): LocalDate = this.withZoneSameInstant(UK_ZONE).toLocalDate()
fun ZonedDateTime.toUkLocalDateTime(): LocalDateTime = this.withZoneSameInstant(UK_ZONE).toLocalDateTime()
fun ZonedDateTime.withUkZone(): ZonedDateTime = this.withZoneSameInstant(UK_ZONE)
