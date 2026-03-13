package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.AddressContactMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.AddressMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.AddressUsageMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.AliasMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.IdentifierMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.PersonContactMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconUpdatePersonResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressUsageRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ContactRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.SentenceInfoRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class SysconPersonUpdateHandler(
  private val personService: PersonService,
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val addressUsageRepository: AddressUsageRepository,
  private val contactRepository: ContactRepository,
  private val pseudonymRepository: PseudonymRepository,
  private val referenceRepository: ReferenceRepository,
  private val sentenceInfoRepository: SentenceInfoRepository,
) {

  @Transactional
  fun handle(prisonNumber: String, prisoner: Prisoner): SysconUpdatePersonResponse = personRepository.findByPrisonNumber(prisonNumber)?.let { personEntity ->
    val result = SysconUpdatePersonResponse(
      prisonerId = prisonNumber,
      addressMappings = overwriteAddresses(personEntity, prisoner),
      personContactMappings = overwriteContacts(personEntity, prisoner),
      pseudonymMappings = overwritePseudonym(personEntity, prisoner),
    ).also {
      overwriteSentenceInfo(personEntity, prisoner)

      // update root level person details only

      // trigger person match, recluster, telemetry?!?
    }
    result
  } ?: throw ResourceNotFoundException("Prisoner not found $prisonNumber")

  fun overwriteAddresses(personEntity: PersonEntity, prisoner: Prisoner): List<AddressMapping> {
    addressRepository.deleteAllByPerson(personEntity)

    val addressMappings = mutableListOf<AddressMapping>()
    prisoner.addresses.forEach { sysconAddress ->
      val coreAddress = Address.from(sysconAddress).copy(usages = emptyList(), contacts = emptyList())
      val addressEntity = addressRepository.save(AddressEntity.from(personEntity, coreAddress))

      val addressUsageMappings = mutableListOf<AddressUsageMapping>()
      sysconAddress.addressUsage.forEach { sysconAddressUsage ->
        val coreAddressUsage = AddressUsage.from(sysconAddressUsage)
        val addressUsageEntity = addressUsageRepository.save(AddressUsageEntity.from(addressEntity, coreAddressUsage))
        addressUsageMappings.add(
          AddressUsageMapping(
            nomisAddressUsageId = sysconAddress.nomisAddressId.toString(),
            cprAddressUsageid = addressUsageEntity.updateId.toString(),
          ),
        )
      }
      val addressContactMappings = mutableListOf<AddressContactMapping>()
      sysconAddress.contacts.forEach { sysconContact ->
        val coreContact = Contact.from(sysconContact) ?: return@forEach // TODO: may break reconciliations
        val contactEntity = contactRepository.save(ContactEntity.from(personEntity, addressEntity, coreContact))
        addressContactMappings.add(
          AddressContactMapping(
            nomisContactId = sysconContact.nomisContactId.toString(),
            cprContactId = contactEntity.updateId.toString(),
          ),
        )
      }
      addressMappings.add(
        AddressMapping(
          nomisAddressId = sysconAddress.nomisAddressId.toString(),
          cprAddressId = addressEntity.updateId.toString(),
          addressUsageMappings = addressUsageMappings,
          addressContactMappings = addressContactMappings,
        ),
      )
    }
    return addressMappings
  }

  private fun overwriteContacts(personEntity: PersonEntity, prisoner: Prisoner): List<PersonContactMapping> {
    contactRepository.deleteAllByPerson(personEntity) // TODO: may not be needed here
    val contactMappings = mutableListOf<PersonContactMapping>()
    prisoner.personContacts.forEach { sysconContact ->
      val coreContact = Contact.from(sysconContact) ?: return@forEach // TODO: may break reconciliations
      val contactEntity = contactRepository.save(ContactEntity.from(personEntity, coreContact))
      contactMappings.add(
        PersonContactMapping(
          nomisContactId = sysconContact.nomisContactId.toString(),
          cprContactId = contactEntity.updateId.toString(),
        ),
      )
    }
    return contactMappings
  }

  private fun overwritePseudonym(personEntity: PersonEntity, prisoner: Prisoner): List<AliasMapping> {
    pseudonymRepository.deleteAllByPerson(personEntity)
    referenceRepository.deleteAllByPerson(personEntity)
    val aliasMappings = mutableListOf<AliasMapping>()
    prisoner.aliases.forEach { sysconAlias ->
      val coreAlias = Alias.from(sysconAlias)
      val pseudonymEntity = if (sysconAlias.isPrimary == true) coreAlias.primaryNameFrom(personEntity) else PseudonymEntity.aliasFrom(personEntity, coreAlias)
      if (pseudonymEntity == null) return@forEach // TODO: may break reconciliations
      val aliasesEntity = pseudonymRepository.save(pseudonymEntity)

      val referenceMappings = mutableListOf<IdentifierMapping>()
      sysconAlias.identifiers.forEach { sysconIdentifier ->
        val coreReference = Reference.from(sysconIdentifier)
        val referenceEntity = referenceRepository.save(ReferenceEntity.from(personEntity, coreReference))
        referenceMappings.add(
          IdentifierMapping(
            nomisIdentifierId = sysconIdentifier.nomisIdentifierId.toString(),
            cprIdentifierId = referenceEntity.updateId.toString(),
          ),
        )
      }

      aliasMappings.add(
        AliasMapping(
          nomisPseudonymId = sysconAlias.nomisAliasId.toString(),
          cprPseudonymId = aliasesEntity.updateId.toString(),
          identifierMappings = referenceMappings,
        ),
      )
    }
    return aliasMappings
  }

  private fun overwriteSentenceInfo(personEntity: PersonEntity, prisoner: Prisoner) {
    sentenceInfoRepository.deleteAllByPerson(personEntity)
    prisoner.sentences.forEach { sysconSentence ->
      // TODO: may break reconciliations
      val sentenceInfoEntity = sysconSentence.sentenceDate?.let { SentenceInfoEntity.from(personEntity, it) } ?: return@forEach
      sentenceInfoRepository.save(sentenceInfoEntity)
    }
  }

  fun Alias.primaryNameFrom(personEntity: PersonEntity): PseudonymEntity = PseudonymEntity(
    person = personEntity,
    firstName = this.firstName,
    middleNames = this.middleNames,
    lastName = this.lastName,
    dateOfBirth = this.dateOfBirth,
    nameType = NameType.PRIMARY,
    titleCode = this.titleCode,
    sexCode = this.sexCode,
  )

//  private fun deletePersonChildTables(personEntity: PersonEntity) {
//    val person = Person.from(personEntity)
//      .copy(addresses = emptyList(), contacts = emptyList(), aliases = emptyList(), references = emptyList())
//
//    // TODO: would trigger premature telemetry event at this point
//    personService.processPerson(person) { personEntity }
//  }
}
