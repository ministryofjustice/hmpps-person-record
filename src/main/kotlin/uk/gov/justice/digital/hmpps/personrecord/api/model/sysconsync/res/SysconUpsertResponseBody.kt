package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.res

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class SysconUpsertResponseBody(
  val prisonerId: String,
  val addressMappings: List<AddressMapping>,
  val personContactMappings: List<PersonContactMapping>,
  val aliasMappings: List<AliasMapping>,
) {

  companion object {
    fun from(personEntity: PersonEntity): SysconUpsertResponseBody {
      return SysconUpsertResponseBody(
        prisonerId = TODO(),
        addressMappings = TODO(),
        personContactMappings = TODO(),
        aliasMappings = TODO()
      )
    }
  }
}
