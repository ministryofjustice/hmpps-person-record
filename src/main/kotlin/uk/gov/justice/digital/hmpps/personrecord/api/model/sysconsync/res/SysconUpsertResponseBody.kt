package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.res

data class SysconUpsertResponseBody(
  val prisonId: String?,
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
