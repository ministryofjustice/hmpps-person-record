package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class SysconUpdatePersonResponse(
  val prisonerId: String?,
  val addressMappings: List<AddressMapping>,
  val personContactMappings: List<PersonContactMapping>,
  val pseudonymMappings: List<AliasMapping>,
) {

  companion object {
    fun from(personEntity: PersonEntity): SysconUpdatePersonResponse = SysconUpdatePersonResponse(
      prisonerId = personEntity.prisonNumber,
      addressMappings = emptyList(),
      personContactMappings = emptyList(),
      pseudonymMappings = emptyList(),
    )
  }
}
