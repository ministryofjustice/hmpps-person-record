package uk.gov.justice.digital.hmpps.personrecord.api.handler.search

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.PersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchData
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchMatchStatus.Companion.toSearchStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchName
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchReference
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

  fun search(personSearchRequest: PersonSearchRequest): PersonSearchResponse {
    val personMatchScoresSortedDescending = getPersonMatchScoresSortedByMatchWeightDescending(personSearchRequest)
    val strongestPersonsAcrossUniqueClusters = findStrongestPersonsAcrossUniqueClusters(personMatchScoresSortedDescending)
    return buildSearchResult(strongestPersonsAcrossUniqueClusters)
  }

  private fun getPersonMatchScoresSortedByMatchWeightDescending(personSearchRequest: PersonSearchRequest) = personMatchClient.search(personSearchRequest)
    .sortedByDescending { it.candidateMatchWeight }

  private fun findStrongestPersonsAcrossUniqueClusters(personMatchScoresSortedDescending: List<PersonMatchScore>) = personMatchScoresSortedDescending
    .map { personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!! }
    .distinctBy { it.personKey!!.id!! }

  private fun buildSearchResult(personEntities: List<PersonEntity>): PersonSearchResponse {
    val searchDataOrderedByMatchProbability = personEntities.map { personEntity ->
      val rootPersonData = toSearchData(personEntity)
      val childPersonData = personEntity.personKey!!.personEntities
        .filter { it != personEntity }
        .map { toSearchData(it) }
      rootPersonData.linkedRecords = childPersonData
      rootPersonData
    }
    return PersonSearchResponse(searchDataOrderedByMatchProbability)
  }

  fun toSearchData(personEntity: PersonEntity): SearchData {
    val mainPseudonym = personEntity.getPrimaryName()
    return SearchData(
      name = SearchName(
        firstName = mainPseudonym.firstName,
        middleNames = mainPseudonym.middleNames,
        lastName = mainPseudonym.lastName,
        dateOfBirth = mainPseudonym.dateOfBirth,
      ),
      aliases = personEntity.getAliases().map { SearchAlias.from(it) },
      addresses = personEntity.addresses.map { SearchAddress.from(it) },
      identifiers = personEntity.references.map { SearchReference.from(it) },
      sourceSystem = personEntity.sourceSystem,
      status = personEntity.personKey!!.status.toSearchStatus(),
    )
  }
}
