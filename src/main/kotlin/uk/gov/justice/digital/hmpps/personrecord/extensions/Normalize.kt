package uk.gov.justice.digital.hmpps.personrecord.extensions

fun String?.nullIfBlank(): String? = this?.ifBlank { null }
