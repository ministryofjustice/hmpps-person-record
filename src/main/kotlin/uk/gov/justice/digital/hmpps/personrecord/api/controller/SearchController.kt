package uk.gov.justice.digital.hmpps.personrecord.api.controller

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.api.model.PersonIdentifierRecord
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

@RestController
@PreAuthorize("hasRole('ROLE_CORE_PERSON_RECORD_API__SEARCH__RO')")
class SearchController(
  private val personRepository: PersonRepository,
) {

  @GetMapping("/search/{personId}")
  suspend fun searchByPersonId(@PathVariable(name = "personId") personId: String): List<PersonIdentifierRecord> {
    val personRecord = getPersonRecord(personId)
    return when {
      personRecord != PersonEntity.empty -> buildListOfLinkedRecords(personRecord!!)
      else -> throw PersonRecordNotFoundException(personId)
    }
  }

  private fun buildListOfLinkedRecords(personEntity: PersonEntity): List<PersonIdentifierRecord> {
    return personEntity.personKey?.let { personKeyEntity ->
      personKeyEntity.personEntities.map {
        buildIdentifierRecord(it)
      }
    } ?: listOf(buildIdentifierRecord(personEntity))
  }

  private fun buildIdentifierRecord(personEntity: PersonEntity): PersonIdentifierRecord {
    val identifier = when (personEntity.sourceSystem) {
      SourceSystemType.DELIUS -> personEntity.crn
      SourceSystemType.NOMIS -> personEntity.prisonNumber
      SourceSystemType.COMMON_PLATFORM -> personEntity.defendantId
      else -> ""
    } ?: ""
    return PersonIdentifierRecord(id = identifier, sourceSystem = personEntity.sourceSystem.name)
  }

  private suspend fun getPersonRecord(personId: String): PersonEntity? = coroutineScope {
    val deferredProbationPersonSearch = async { personRepository.findByCrnAndSourceSystem(personId) }
    val deferredPrisonPersonSearch = async { personRepository.findByPrisonNumberAndSourceSystem(personId) }
    val deferredCommonPlatformPersonSearch = async { personRepository.findByDefendantIdAndSourceSystem(personId) }
    return@coroutineScope awaitAll(
      deferredProbationPersonSearch,
      deferredPrisonPersonSearch,
      deferredCommonPlatformPersonSearch
    ).filterNotNull().firstOrNull()
  }
}