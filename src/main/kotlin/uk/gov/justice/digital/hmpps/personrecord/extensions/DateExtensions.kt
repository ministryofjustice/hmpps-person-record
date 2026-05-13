package uk.gov.justice.digital.hmpps.personrecord.extensions

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

val UK_ZONE: ZoneId = ZoneId.of("Europe/London")

// LocalDate extensions
fun LocalDate.toOffsetDateTime(): OffsetDateTime = this.atStartOfDay(UK_ZONE).toOffsetDateTime()

// OffsetDateTime extensions
fun OffsetDateTime.toUkLocalDate(): LocalDate = this.atZoneSameInstant(UK_ZONE).toLocalDate()
fun OffsetDateTime.toUkOffsetDateTime(): OffsetDateTime = this.atZoneSameInstant(UK_ZONE).toOffsetDateTime()
