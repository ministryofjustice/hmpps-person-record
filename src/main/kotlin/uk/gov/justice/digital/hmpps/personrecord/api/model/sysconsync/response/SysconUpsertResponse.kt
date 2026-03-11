package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class SysconUpsertResponse(
  val prisonerId: String?,
  val addressMappings: List<AddressMapping>,
  val personContactMappings: List<PersonContactMapping>,
  val pseudonymMappings: List<AliasMapping>,
) {

  companion object {
    fun from(personEntity: PersonEntity): SysconUpsertResponse = SysconUpsertResponse(
      prisonerId = TODO(),
      addressMappings = TODO(),
      personContactMappings = TODO(),
      pseudonymMappings = TODO(),
    )
  }
}
