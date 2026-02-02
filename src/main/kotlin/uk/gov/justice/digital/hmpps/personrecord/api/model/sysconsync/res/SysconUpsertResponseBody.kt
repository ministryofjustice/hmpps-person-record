package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.res

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class SysconUpsertResponseBody(
  val prisonerId: String?,
  val addressMappings: List<AddressMapping>,
  val personContactMappings: List<PersonContactMapping>,
  val aliasMappings: List<AliasMapping>,
) {

//  companion object {
//    fun from(personEntity: PersonEntity): SysconUpsertResponseBody {
//
//
//      return SysconUpsertResponseBody(
//        prisonerId = personEntity.prisonNumber,
//        addressMappings = personEntity.addresses.map { addressEntity ->
//          AddressMapping(
//            nomisAddressId = addressEntity.nomisAddressId,
//            cprAddressId = addressEntity.id,
//            addressUsageMappings = addressEntity.usages.map { addressUsageEntity ->
//              AddressUsageMapping(
//                nomisAddressUsageId = addressUsageEntity.nomisAddressUsageId,
//                cprAddressUsageid = addressUsageEntity.id,
//              )
//            },
//            addressContactMappings = addressEntity.contacts.map { addressContactEntity ->
//              AddressContactMapping(
//                nomisAddressContactId = addressContactEntity.nomisAddressContactId,
//                cprAddressContactId = addressContactEntity.id,
//              )
//            },
//          )
//        },
//        personContactMappings = personEntity.contacts.map { personContactEntity ->
//          PersonContactMapping(
//            nomisPersonContactId = personContactEntity.nomisPersonContactId,
//            cprPersonContactId = personContactEntity.id,
//          )
//        },
//        aliasMappings = personEntity.pseudonyms.map { pseudonymEntity ->
//          AliasMapping(
//            nomisAliasId = pseudonymEntity.nomisAliasId,
//            cprAliasId = pseudonymEntity.id,
//            identifierMappings = emptyList() // NOTE: map identifiers/references
//          )
//        },
//      )
//    }
//  }
}
