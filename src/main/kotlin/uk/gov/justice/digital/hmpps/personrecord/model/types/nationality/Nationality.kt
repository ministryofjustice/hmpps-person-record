package uk.gov.justice.digital.hmpps.personrecord.model.types.nationality

data class Nationality(
  val nationalityCode: NationalityCode,
  val notes: String? = null,
)
