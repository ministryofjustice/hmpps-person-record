package uk.gov.justice.digital.hmpps.personrecord.extensions

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun nowUtcFormattedUk(): String? = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/London")).format(Instant.now())
