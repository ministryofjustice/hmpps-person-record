package uk.gov.justice.digital.hmpps.personrecord.extentions

fun String?.nullIfBlank(): String? = this?.ifBlank { null }
