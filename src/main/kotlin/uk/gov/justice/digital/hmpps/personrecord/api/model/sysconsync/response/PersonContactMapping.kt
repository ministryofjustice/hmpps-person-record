package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class PersonContactMapping(
  val nomisPersonContactId: Long,
  val cprPersonContactId: Long?,
)
