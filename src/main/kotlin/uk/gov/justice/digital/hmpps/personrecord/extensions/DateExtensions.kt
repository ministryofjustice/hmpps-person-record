package uk.gov.justice.digital.hmpps.personrecord.extensions

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

fun LocalDate.toOffsetDateTime(): OffsetDateTime = this.atStartOfDay(ZoneId.of("Europe/London"))
  .toOffsetDateTime()

fun OffsetDateTime.toUkLocalDate(): LocalDate = this.atZoneSameInstant(ZoneId.of("Europe/London")).toLocalDate()
