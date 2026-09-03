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
    val referenceMappings = handleReferencesInsert(prisonAliasesAndIdentifiersRequest.identifiers, person)
    val pseudonymMappings = handlePseudonymsInsert(prisonAliasesAndIdentifiersRequest.aliases, person)

    val currentPseudonym = prisonAliasesAndIdentifiersRequest.aliases.first { it.isPrimary == true }
    person.ethnicityCode = currentPseudonym.ethnicity
    person.birthCountryCode = currentPseudonym.birthCountry
    person.birthplace = currentPseudonym.birthPlace
    val updatedPerson = personRepository.saveAndFlush(person)
    personService.processPerson(Person.from(updatedPerson)) { updatedPerson }

    return SysconAliasesAndIdentifiersResponseBody(
      aliasesMappings = pseudonymMappings,
      identifiersMappings = referenceMappings,
      prisonNumber = prisonNumber,
    )
  }

  private fun handlePseudonymsInsert(pseudonyms: List<PrisonAlias>, personEntity: PersonEntity): List<SysconAliasMapping> {
    val pseudonymEntities = pseudonymRepository.saveAllAndFlush(pseudonyms.map { it.toEntity(personEntity) }) //  Guarantee ordering
    personEntity.updatePseudonyms(pseudonymEntities)
    personRepository.saveAndFlush(personEntity)
    val pseudonymMappings = pseudonyms
      .zip(pseudonymEntities)
      .map { (alias, entity) -> SysconAliasMapping(alias.nomisOffenderId, entity.updateId.toString()) }
    return pseudonymMappings
  }

  private fun handleReferencesInsert(references: List<PrisonIdentifier>, personEntity: PersonEntity): List<SysconIdentifierMapping> {
    val referenceEntities = referenceRepository.saveAllAndFlush(references.map { it.toEntity(personEntity) }) // Guarantee ordering
    personEntity.updatePersonReferences(referenceEntities)
    personRepository.saveAndFlush(personEntity)
    val referenceMappings = references
      .zip(referenceEntities)
      .map { (identifier, entity) -> SysconIdentifierMapping(identifier.nomisIdentifierId.toId(), entity.updateId.toString()) }
    return referenceMappings
  }

  private fun validatePseudonyms(prisonNumber: String, pseudonyms: List<PrisonAlias>) {
    if (pseudonyms.isEmpty()) {
      throw IllegalArgumentException("At least one pseudonym must be sent for $prisonNumber")
    }
    val primaryPseudonym = pseudonyms.filter { it.isPrimary ?: false }
    if (primaryPseudonym.size != 1) {
      throw IllegalArgumentException("There must be exactly one primary pseudonym for $prisonNumber")
    }
    val pseudonymNomisIdDuplicates = pseudonyms.groupingBy { it.nomisOffenderId }.eachCount().filter { it.value > 1 }
    if (pseudonymNomisIdDuplicates.isNotEmpty()) {
      throw IllegalArgumentException("Duplicate nomis pseudonym ids were detected for $prisonNumber: ${pseudonymNomisIdDuplicates.keys.joinToString()}")
    }
  }

  private fun validateReferences(prisonNumber: String, references: List<PrisonIdentifier>) {
    val referenceDuplicates = references.map { it.nomisIdentifierId }.groupingBy { it.nomisOffenderId to it.nomisSequence }.eachCount().filter { it.value > 1 }
    if (referenceDuplicates.isNotEmpty()) {
      throw IllegalArgumentException(
        "Duplicate nomis reference ids were detected for $prisonNumber: ${referenceDuplicates.keys.joinToString { "${it.first}-${it.second}" }}",
      )
    }
  }

  private fun validateRequest(prisonNumber: String, prisonAliasesAndIdentifiersRequest: PrisonAliasesAndIdentifiersRequest) {
    validatePseudonyms(prisonNumber, prisonAliasesAndIdentifiersRequest.aliases)
    validateReferences(prisonNumber, prisonAliasesAndIdentifiersRequest.identifiers)
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
