package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAliasesAndIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasesAndIdentifiersResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconIdentifierMapping
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.NomisIdentifierId as RequestNomisIdentifierId
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.NomisIdentifierId as ResponseNomisIdentifierId

@Profile("!prod")
@Component
class SysconAliasesAndIdentifiersMigrationHandler(
  private val personRepository: PersonRepository,
  private val pseudonymRepository: PseudonymRepository,
  private val referenceRepository: ReferenceRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleInsert(
    prisonNumber: String,
    prisonAliasesAndIdentifiersRequest: PrisonAliasesAndIdentifiersRequest,
  ): SysconAliasesAndIdentifiersResponseBody {
    validateRequest(prisonNumber, prisonAliasesAndIdentifiersRequest)
    val person = personRepository.findByPrisonNumber(prisonNumber) ?: throw IllegalArgumentException("Person with $prisonNumber not found")
    val identifierMappings = handleIdentifiersInsert(prisonAliasesAndIdentifiersRequest.identifiers, person)
    val aliasMappings = handleAliasesInsert(prisonAliasesAndIdentifiersRequest.aliases, person)

    val currentAlias = prisonAliasesAndIdentifiersRequest.aliases.first { it.isPrimary == true }
    person.ethnicityCode = currentAlias.ethnicity
    person.birthCountryCode = currentAlias.birthCountry
    person.birthplace = currentAlias.birthPlace
    val updatedPerson = personRepository.saveAndFlush(person)
    personService.processPerson(Person.from(updatedPerson)) { updatedPerson }

    return SysconAliasesAndIdentifiersResponseBody(
      aliasesMappings = aliasMappings,
      identifiersMappings = identifierMappings,
      prisonNumber = prisonNumber,
      cprId = prisonNumber,
    )
  }

  private fun handleAliasesInsert(aliases: List<PrisonAlias>, personEntity: PersonEntity): List<SysconAliasMapping> {
    val aliasEntities = pseudonymRepository.saveAllAndFlush(aliases.map { it.toEntity(personEntity) }) //  Guarantee ordering
    personEntity.updatePseudonyms(aliasEntities)
    personRepository.saveAndFlush(personEntity)
    val aliasMappings = aliases
      .zip(aliasEntities)
      .map { (alias, entity) -> SysconAliasMapping(alias.nomisOffenderId, entity.updateId.toString()) }
    return aliasMappings
  }

  private fun handleIdentifiersInsert(identifiers: List<PrisonIdentifier>, personEntity: PersonEntity): List<SysconIdentifierMapping> {
    val identifierEntities = referenceRepository.saveAllAndFlush(identifiers.map { it.toEntity(personEntity) }) // Guarantee ordering
    personEntity.updatePersonReferences(identifierEntities)
    personRepository.saveAndFlush(personEntity)
    val identifierMappings = identifiers
      .zip(identifierEntities)
      .map { (identifier, entity) -> SysconIdentifierMapping(identifier.nomisIdentifierId.toId(), entity.updateId.toString()) }
    return identifierMappings
  }

  private fun validateRequest(prisonNumber: String, prisonAliasesAndIdentifiersRequest: PrisonAliasesAndIdentifiersRequest) {
    val aliases = prisonAliasesAndIdentifiersRequest.aliases
    if (aliases.isEmpty()) {
      throw IllegalArgumentException("At least one alias must be sent for $prisonNumber")
    }
    val currentAliases = aliases.filter { it.isPrimary ?: false }
    if (currentAliases.size != 1) {
      throw IllegalArgumentException("There must be exactly one primary alias for $prisonNumber")
    }
    val aliasDuplicates = aliases.groupingBy { it.nomisOffenderId }.eachCount().filter { it.value > 1 }
    if (aliasDuplicates.isNotEmpty()) {
      throw IllegalArgumentException("Duplicate nomis alias ids were detected for $prisonNumber: ${aliasDuplicates.keys.joinToString()}")
    }
    val identifiers = prisonAliasesAndIdentifiersRequest.identifiers
    val identifierDuplicates = identifiers.map { it.nomisIdentifierId }.groupingBy { it.nomisOffenderId to it.nomisSequence }.eachCount().filter { it.value > 1 }
    if (identifierDuplicates.isNotEmpty()) {
      throw IllegalArgumentException(
        "Duplicate nomis identifier ids were detected for $prisonNumber: ${identifierDuplicates.keys.joinToString { "${it.first}-${it.second}" }}",
      )
    }
  }

  /**
   * Note: The following fields are not mapped to the PseudonymEntity:
   * - createDate: This field is not mapped.
   * - birthCountry: This field is mapped to the PersonEntity.
   * - birthPlace: This field is mapped to the PersonEntity.
   * - ethnicity: This field is mapped to the PersonEntity.
   */
  fun PrisonAlias.toEntity(personEntity: PersonEntity): PseudonymEntity = PseudonymEntity(
    titleCode = titleCode,
    firstName = firstName,
    middleNames = middleNames,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    sexCode = sexCode,
    nameType = if (isPrimary == true) NameType.PRIMARY else NameType.ALIAS,
    person = personEntity,
  )

  /**
   * Note: The following fields are not mapped to the ReferenceEntity:
   * - issuedDate: This field is not mapped.
   * - verified: This field is not mapped.
   */
  fun PrisonIdentifier.toEntity(personEntity: PersonEntity): ReferenceEntity = ReferenceEntity(
    identifierType = type,
    identifierValue = value,
    comment = comment,
    person = personEntity,
  )

  fun RequestNomisIdentifierId.toId(): ResponseNomisIdentifierId = ResponseNomisIdentifierId(nomisOffenderId, nomisSequence)
}
