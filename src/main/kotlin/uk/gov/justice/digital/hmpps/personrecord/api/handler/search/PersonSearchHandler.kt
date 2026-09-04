package uk.gov.justice.digital.hmpps.personrecord.api.handler.search

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchData
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.util.UUID

@Component
class PersonSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  fun search(personSearchRequest: PersonSearchRequest): PersonSearchResponse {
    val personMatchScoresSortedDescending = getPersonMatchScoresSortedByMatchWeightDescending(personSearchRequest)
    val strongestPersonsAcrossUniqueClusters = findStrongestPersonsAcrossUniqueClusters(personMatchScoresSortedDescending)
    return buildSearchResult(strongestPersonsAcrossUniqueClusters)
  }

  private fun getPersonMatchScoresSortedByMatchWeightDescending(personSearchRequest: PersonSearchRequest) = personMatchClient.search(PersonMatchSearchRequest.from(personSearchRequest))
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
