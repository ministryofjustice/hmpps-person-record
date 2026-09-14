package uk.gov.justice.digital.hmpps.personrecord.api.handler.search

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchData
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.util.UUID

@Component
class PersonSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  fun search(personSearchRequest: VettingPersonSearchRequest): VettingPersonSearchResponse {
    val personMatchScores = getPersonMatchScores(personSearchRequest)
    val personsByMatchScoreSortedDescending = collectPersonsClusterWideByMatchScoreSortedDescending(personMatchScores)
    val strongestPersonsAcrossUniqueClustersSortedDescending = findStrongestPersonsAcrossUniqueClusters(personsByMatchScoreSortedDescending)

    return buildSearchResult(strongestPersonsAcrossUniqueClustersSortedDescending)
  }

  private fun getPersonMatchScores(personSearchRequest: VettingPersonSearchRequest) = personMatchClient.search(PersonMatchSearchRequest.from(personSearchRequest))

  private fun collectPersonsClusterWideByMatchScoreSortedDescending(personMatchScores: List<PersonMatchScore>): Map<PersonMatchScore, PersonEntity> {
    val returnedScoresFromPersonMatch = personMatchScores.associateBy { it.candidateMatchId }
    val personsByMatchScore = mutableMapOf<PersonMatchScore, PersonEntity>()

    personMatchScores.forEach {
      val personEntitiesInCluster = personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!!.personKey!!.personEntities
      personEntitiesInCluster
        .filter { personEntity -> personEntity.sourceSystem != SourceSystemType.COMMON_PLATFORM && personEntity.sourceSystem != SourceSystemType.LIBRA }
        .forEach { personEntity ->
          val personMatchId = personEntity.matchId.toString()
          if (returnedScoresFromPersonMatch.containsKey(personMatchId)) {
            val personMatchScore = returnedScoresFromPersonMatch[personMatchId]!!
            personsByMatchScore[personMatchScore] = personEntity
          } else {
            getMatchScoreForPerson(personEntity)?.let { score -> personsByMatchScore[score] = personEntity }
          }
        }
    }
    return personsByMatchScore.toSortedMap(compareByDescending { it.candidateMatchWeight })
  }

  private fun getMatchScoreForPerson(personEntity: PersonEntity): PersonMatchScore? {
    val fullName = """${personEntity.getPrimaryName().firstName} ${personEntity.getPrimaryName().middleNames} ${personEntity.getPrimaryName().lastName}"""
    val request = personEntity.getPrimaryName().dateOfBirth?.let { dateOfBirth ->
      PersonMatchSearchRequest(
        fullName = fullName,
        dateOfBirth = dateOfBirth,
        postcodes = personEntity.addresses.mapNotNull { addressEntity -> addressEntity.postcode },
      )
    }
    return if (request != null) {
      val personMatchScores = personMatchClient.search(request)
      personMatchScores.firstOrNull { it.candidateMatchId == personEntity.matchId.toString() }
    } else {
      null
    }
  }

  private fun findStrongestPersonsAcrossUniqueClusters(personsByMatchScoreSortedDescending: Map<PersonMatchScore, PersonEntity>) = personsByMatchScoreSortedDescending.values
    .distinctBy { it.personKey!!.id!! }

  private fun buildSearchResult(strongestPersonsAcrossUniqueClustersSortedDescending: List<PersonEntity>): VettingPersonSearchResponse {
    val searchDataOrderedByMatchProbability = strongestPersonsAcrossUniqueClustersSortedDescending.map { personEntity ->
      val rootPersonData = SearchData.from(personEntity)
      val childPersonData = personEntity.personKey!!.personEntities
        .filter { it != personEntity }
        .filter { it.sourceSystem != SourceSystemType.COMMON_PLATFORM && it.sourceSystem != SourceSystemType.LIBRA }
        .map { SearchData.from(it) }
      rootPersonData.linkedRecords = childPersonData
      rootPersonData
    }
    return VettingPersonSearchResponse(searchDataOrderedByMatchProbability)
  }
}
