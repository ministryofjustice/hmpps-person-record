package uk.gov.justice.digital.hmpps.personrecord.extensions

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

val UK_ZONE: ZoneId = ZoneId.of("Europe/London")

// LocalDate extensions
fun LocalDate.toUkZonedDateTime(): ZonedDateTime = this.atStartOfDay(UK_ZONE)

// ZonedDateTime extensions
fun ZonedDateTime.toUkLocalDate(): LocalDate = this.withZoneSameInstant(UK_ZONE).toLocalDate()
