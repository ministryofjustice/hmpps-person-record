package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

data class ReferenceDataResponse(
  val code: String,
  val description: String,
  val active: Boolean,
)
