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
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import java.time.LocalDate

@Component
class SysconPersonUpdateHandler(
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
    updateRootPersonOnlyAndDeletePersonChildTables(personEntity, prisoner)
    val updateResult = SysconUpdatePersonResponse(
      prisonerId = prisonNumber,
      addressMappings = overwriteAddresses(personEntity, prisoner),
      personContactMappings = overwriteContacts(personEntity, prisoner),
      pseudonymMappings = overwritePseudonym(personEntity, prisoner),
    ).also {
      overwriteSentenceInfo(personEntity, prisoner)

      // trigger person match, recluster, telemetry?!?
    }
    updateResult
  } ?: throw ResourceNotFoundException("Prisoner not found $prisonNumber")

  fun overwriteAddresses(personEntity: PersonEntity, prisoner: Prisoner): List<AddressMapping> = prisoner.addresses.map { sysconAddress ->
    val coreAddress = Address.from(sysconAddress).copy(usages = emptyList(), contacts = emptyList())
    val addressEntity = addressRepository.save(coreAddress.toEntity(personEntity))

    val addressUsageMappings = sysconAddress.addressUsage.map { sysconAddressUsage ->
      val coreAddressUsage = AddressUsage.from(sysconAddressUsage)
      val addressUsageEntity = addressUsageRepository.save(coreAddressUsage.toEntity(addressEntity))
      AddressUsageMapping(
        nomisAddressUsageId = sysconAddressUsage.nomisAddressUsageId.toString(),
        cprAddressUsageid = addressUsageEntity.updateId.toString(),
      )
    }

    val addressContactMappings = sysconAddress.contacts
      .mapNotNull { sysconAddressContact -> Contact.from(sysconAddressContact)?.let { sysconAddressContact.nomisContactId!!.toString() to it } }
      .map { (nomisAddressContactId, coreContact) ->
        val contactEntity = contactRepository.save(coreContact.toEntity(addressEntity))
        AddressContactMapping(
          nomisContactId = nomisAddressContactId,
          cprContactId = contactEntity.updateId.toString(),
        )
      }

    AddressMapping(
      nomisAddressId = sysconAddress.nomisAddressId.toString(),
      cprAddressId = addressEntity.updateId.toString(),
      addressUsageMappings = addressUsageMappings,
      addressContactMappings = addressContactMappings,
    )
  }

  private fun overwriteContacts(personEntity: PersonEntity, prisoner: Prisoner): List<PersonContactMapping> = prisoner.personContacts
    .mapNotNull { sysconContact -> Contact.from(sysconContact)?.let { sysconContact.nomisContactId!!.toString() to it } }
    .map { (nomisPersonContactId, coreContact) ->
      val contactEntity = contactRepository.save(coreContact.toEntity(personEntity))
      PersonContactMapping(
        nomisContactId = nomisPersonContactId,
        cprContactId = contactEntity.updateId.toString(),
      )
    }

  private fun overwritePseudonym(personEntity: PersonEntity, prisoner: Prisoner): List<AliasMapping> {
    val aliasMappings = mutableListOf<AliasMapping>()
    prisoner.aliases
      .forEach { sysconAlias ->
        val coreAlias = Alias.from(sysconAlias)
        val pseudonymEntity = if (sysconAlias.isPrimary == true) coreAlias.primaryNameFromElseNull(personEntity) else coreAlias.toEntity(personEntity)
        if (pseudonymEntity == null) return@forEach

        val aliasEntity = if (pseudonymEntity.nameType == NameType.PRIMARY) {
          val hardCodedPrimary = pseudonymRepository.findById(personEntity.pseudonyms.first().id!!).orElseThrow()
          hardCodedPrimary.firstName = sysconAlias.firstName
          hardCodedPrimary.middleNames = sysconAlias.middleNames
          hardCodedPrimary.lastName = sysconAlias.lastName
          hardCodedPrimary.titleCode = sysconAlias.titleCode
          hardCodedPrimary.dateOfBirth = sysconAlias.dateOfBirth
          hardCodedPrimary.sexCode = prisoner.demographicAttributes.sexCode
          pseudonymRepository.save(hardCodedPrimary)
        } else {
          pseudonymRepository.save(pseudonymEntity)
        }

        // TODO: these will need to change to link against pseudonym after 1065. (don't forget to take out of alias loop)
        val referenceMappings = mutableListOf<IdentifierMapping>()
        sysconAlias.identifiers.forEach { sysconIdentifier ->
          val coreReference = Reference.from(sysconIdentifier)
          val referenceEntity = referenceRepository.save(coreReference.toEntity(personEntity))
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
            cprPseudonymId = aliasEntity.updateId.toString(),
            identifierMappings = referenceMappings,
          ),
        )
      }
    return aliasMappings
  }

  private fun overwriteSentenceInfo(personEntity: PersonEntity, prisoner: Prisoner) {
    prisoner.sentences
      .mapNotNull { sysconSentence -> sysconSentence.sentenceDate?.let { sentenceDate -> sentenceDate.toSentenceInfoEntity(personEntity) } }
      .forEach { sentenceInfoEntity ->
        sentenceInfoRepository.save(sentenceInfoEntity)
      }
  }

  fun Address.toEntity(personEntity: PersonEntity): AddressEntity {
    val addressEntity = AddressEntity.from(this)
    addressEntity.person = personEntity
    return addressEntity
  }

  fun AddressUsage.toEntity(addressEntity: AddressEntity): AddressUsageEntity {
    val addressUsageEntity = AddressUsageEntity.from(this)
    addressUsageEntity.address = addressEntity
    return addressUsageEntity
  }

  fun Contact.toEntity(personEntity: PersonEntity): ContactEntity {
    val contactEntity = ContactEntity.from(this)
    contactEntity.person = personEntity
    return contactEntity
  }

  fun Contact.toEntity(addressEntity: AddressEntity): ContactEntity {
    val contactEntity = ContactEntity.from(this)
    contactEntity.address = addressEntity
    return contactEntity
  }

  fun Alias.toEntity(personEntity: PersonEntity): PseudonymEntity? {
    val aliasEntity = PseudonymEntity.aliasFrom(this)
    aliasEntity?.let { it.person = personEntity }
    return aliasEntity
  }

  private fun Alias.primaryNameFromElseNull(personEntity: PersonEntity) = when {
    sequenceOf(this.firstName, this.middleNames, this.lastName).filterNotNull().any { it.isNotBlank() } -> PseudonymEntity(
      person = personEntity,
      firstName = this.firstName,
      middleNames = this.middleNames,
      lastName = this.lastName,
      dateOfBirth = this.dateOfBirth,
      nameType = NameType.PRIMARY,
      titleCode = this.titleCode,
      sexCode = this.sexCode,
    )
    else -> null
  }

  fun Reference.toEntity(person: PersonEntity): ReferenceEntity {
    val referenceEntity = ReferenceEntity.from(this)
    referenceEntity.person = person
    return referenceEntity
  }

  fun LocalDate.toSentenceInfoEntity(person: PersonEntity): SentenceInfoEntity = SentenceInfoEntity(
    sentenceDate = this,
    person = person,
  )

  private fun updateRootPersonOnlyAndDeletePersonChildTables(personEntity: PersonEntity, prisoner: Prisoner) {
    val person = Person.from(prisoner, personEntity.prisonNumber!!)
      .copy(addresses = emptyList(), contacts = emptyList(), aliases = emptyList(), references = emptyList(), sentences = emptyList())
    personEntity.update(person)
    personRepository.save(personEntity)
  }
}
