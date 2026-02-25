package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconUpsertResponseBody(
  val prisonerId: String?,
  val addressMappings: List<AddressMapping>,
  val personContactMappings: List<PersonContactMapping>,
  val aliasMappings: List<AliasMapping>,
) {

//  companion object {
//    fun from(personEntity: PersonEntity): SysconUpsertResponseBody {
//
//    }
//  }
}
