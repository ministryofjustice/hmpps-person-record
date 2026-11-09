package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

data class ReferenceDataWithActiveResponse(
  val code: String,
  val description: String,
  val active: Boolean,
)
