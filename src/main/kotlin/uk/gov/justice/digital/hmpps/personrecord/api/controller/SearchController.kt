package uk.gov.justice.digital.hmpps.personrecord.api.controller

import jakarta.validation.constraints.NotBlank
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.PersonRecordNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.PersonIdentifierRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

@RestController
@PreAuthorize("hasRole('ROLE_CORE_PERSON_RECORD_API__SEARCH__RO')")
class SearchController(
  private val personRepository: PersonRepository,
) {

  @GetMapping("/search/{personId}")
  suspend fun searchByPersonId(@NotBlank @PathVariable(name = "personId") personId: String): List<PersonIdentifierRecord> {
    val personRecord = getPersonRecord(personId)
    return when {
      personRecord != PersonEntity.empty -> buildListOfLinkedRecords(personRecord!!)
      else -> throw PersonRecordNotFoundException(personId)
    }
  }

  private fun buildListOfLinkedRecords(personEntity: PersonEntity): List<PersonIdentifierRecord> {
    return personEntity.personKey?.personEntities?.mapNotNull {
      buildIdentifierRecord(it)
    } ?: listOfNotNull(buildIdentifierRecord(personEntity))
  }

  private fun buildIdentifierRecord(personEntity: PersonEntity): PersonIdentifierRecord? {
    return when (personEntity.sourceSystem) {
      SourceSystemType.DELIUS -> PersonIdentifierRecord(id = personEntity.crn!!, SourceSystemType.DELIUS.name)
      SourceSystemType.NOMIS -> PersonIdentifierRecord(id = personEntity.prisonNumber!!, SourceSystemType.NOMIS.name)
      SourceSystemType.COMMON_PLATFORM -> PersonIdentifierRecord(id = personEntity.defendantId!!, SourceSystemType.COMMON_PLATFORM.name)
      else -> null
    }
  }

  private suspend fun getPersonRecord(personId: String): PersonEntity? = coroutineScope {
    val deferredProbationPersonSearch = async { personRepository.findByCrn(personId) }
    val deferredPrisonPersonSearch = async { personRepository.findByPrisonNumberAndSourceSystem(personId) }
    val deferredCommonPlatformPersonSearch = async { personRepository.findByDefendantId(personId) }
    return@coroutineScope awaitAll(
      deferredProbationPersonSearch,
      deferredPrisonPersonSearch,
      deferredCommonPlatformPersonSearch,
    ).filterNotNull().firstOrNull()
  }
}
