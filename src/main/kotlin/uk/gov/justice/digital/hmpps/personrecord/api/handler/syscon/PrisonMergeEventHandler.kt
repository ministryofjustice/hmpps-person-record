package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconContactMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService

interface PrisonMergeEventHandler {
  fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody
}

@Profile("prod | preprod")
@Component
class PrisonMergeEventHandlerProd(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler,
) : PrisonMergeEventHandler {

  @Transactional
  override fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody {
    prisonerSearchClient.getPrisoner(toPrisonNumber)?.let {
      val fromPersonEntity = personRepository.findByPrisonNumber(prisonMerge.fromPrisonNumber)!!
      val toPersonEntity = personRepository.findByPrisonNumber(toPrisonNumber)!!
      prisonReligionMergeHandler.handleMerge(fromPersonEntity, toPersonEntity)
      mergeService.processMerge(fromPersonEntity, toPersonEntity, it)
    }
    return SysconPersonMergeResponseBody(toPrisonNumber, SysconAddressesAndContactsResponseBody(toPrisonNumber, emptyList(), emptyList()))
  }
}

@Profile("!preprod & !prod")
@Component
class PrisonMergeEventHandlerDev(
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val contactRepository: ContactRepository,
  private val personMergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler,
) : PrisonMergeEventHandler {

  @Transactional
  override fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody {
    val toPersonCallback = prisonerSearchClient.getPrisoner(toPrisonNumber)!!

    val fromPersonEntity = personRepository.findByPrisonNumber(prisonMerge.fromPrisonNumber)!!
    val toPersonEntity = personRepository.findByPrisonNumber(toPrisonNumber)!!
    val personChangeChecker = PersonChangeChecker(toPersonEntity)

    mergePrisonReligions(fromPersonEntity, toPersonEntity)
    val addressMappings = mergeAddresses(toPersonEntity, prisonMerge)
    val contactMappings = mergeContacts(toPersonEntity, prisonMerge)

    personMergeService.processMerge(
      fromPersonEntity,
      toPersonEntity,
      toPersonCallback,
      personChangeChecker,
      childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
    )

    val dataMappings = SysconAddressesAndContactsResponseBody(toPrisonNumber, addressMappings, contactMappings)
    return SysconPersonMergeResponseBody(toPrisonNumber, dataMappings)
  }

  private fun mergePrisonReligions(fromPersonEntity: PersonEntity, toPersonEntity: PersonEntity) {
    prisonReligionMergeHandler.handleMerge(fromPersonEntity, toPersonEntity)
  }

  private fun mergeAddresses(toPersonEntity: PersonEntity, prisonMerge: PrisonMerge): List<SysconAddressMapping> {
    val toPersonsNewAddressesRepresentation = prisonMerge.toPrisonAddressesAndContactsRequest.addresses
    val toPersonAddressesBeforeMerge = toPersonEntity.addresses.associateBy { it.updateId.toString() }
    toPersonEntity.addresses.clear()

    val addressMappings = toPersonsNewAddressesRepresentation.map { prisonAddress ->
      val addressEntity = toPersonAddressesBeforeMerge[prisonAddress.cprAddressId] ?: AddressEntity.from(Address.from(prisonAddress))

      val addressContactsBeforeMerge = addressEntity.contacts.associateBy { it.updateId.toString() }
      val addressContactMappings = prisonAddress.contacts.map { prisonAddressContact ->
        val contactEntity = addressContactsBeforeMerge[prisonAddressContact.cprContactId] ?: ContactEntity.from(Contact.from(prisonAddressContact)!!)
        contactEntity.address = addressEntity
        addressEntity.contacts.add(contactEntity)
        contactRepository.save(contactEntity)
        SysconContactMapping(
          nomisContactId = prisonAddressContact.nomisContactId!!,
          nomisContactType = prisonAddressContact.type,
          cprContactId = contactEntity.updateId.toString(),
        )
      }

      addressEntity.person = toPersonEntity
      toPersonEntity.addresses.add(addressEntity)
      addressRepository.save(addressEntity)
      SysconAddressMapping(
        nomisAddressId = prisonAddress.nomisAddressId!!,
        cprAddressId = addressEntity.updateId.toString(),
        addressUsageMappings = emptyList(),
        contactMappings = addressContactMappings,
      )
    }
    return addressMappings
  }

  private fun mergeContacts(toPrisonNumber: PersonEntity, prisonMerge: PrisonMerge): List<SysconContactMapping> = emptyList()
}
