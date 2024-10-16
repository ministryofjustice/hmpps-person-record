package uk.gov.justice.digital.hmpps.personrecord.controller

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@RestController
class PersonRecordSearchController(
  private val personRepository: PersonRepository,
) {

  @GetMapping("/search/{personId}")
  suspend fun searchByPersonId(@PathVariable(name = "personId") personId: String) {
    val personRecord = getPersonRecord(personId)
    when {
      personRecord != PersonEntity.empty -> {}
      else -> throw PersonRecordNotFoundException(personId = personId)
    }
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