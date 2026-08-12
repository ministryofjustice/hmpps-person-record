package uk.gov.justice.digital.hmpps.personrecord.api.handler.search

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchData
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.util.UUID

@Component
class PersonSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  private val logger = LoggerFactory.getLogger(PersonSearchHandler::class.java)

  fun search(personSearchRequest: PersonSearchRequest): PersonSearchResponse {
    logger.info("===========================")
    val personMatchScoresSortedDescending = getPersonMatchScoresSortedByMatchWeightDescending(personSearchRequest)
    logger.info("Total matches found ${personMatchScoresSortedDescending.size}")
    personMatchScoresSortedDescending.forEach { logger.info(it.toString()) }
    val strongestPersonsAcrossUniqueClusters = findStrongestPersonsAcrossUniqueClusters(personMatchScoresSortedDescending)
    logger.info("Strongest matches ${strongestPersonsAcrossUniqueClusters.size}")
    strongestPersonsAcrossUniqueClusters.forEach { logger.info(it.toString()) }
    logger.info("===========================")
    return buildSearchResult(strongestPersonsAcrossUniqueClusters)
  }

  private fun getPersonMatchScoresSortedByMatchWeightDescending(personSearchRequest: PersonSearchRequest) = personMatchClient.searchPerson(personSearchRequest)
    .sortedByDescending { it.candidateMatchWeight }

  private fun findStrongestPersonsAcrossUniqueClusters(personMatchScoresSortedDescending: List<PersonMatchScore>) = personMatchScoresSortedDescending
    .map { personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!! }
    .distinctBy { it.personKey!!.id!! }

  private fun buildSearchResult(personEntities: List<PersonEntity>): PersonSearchResponse {
    val searchDataOrderedByMatchProbability = personEntities.map { personEntity ->
      val rootPersonData = SearchData.from(personEntity)
      val childPersonData = personEntity.personKey!!.personEntities
        .filter { it != personEntity }
        .map { SearchData.from(it) }
      rootPersonData.linkedRecords = childPersonData
      rootPersonData
    }
    return PersonSearchResponse(searchDataOrderedByMatchProbability)
  }
}
