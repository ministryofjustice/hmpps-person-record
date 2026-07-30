package uk.gov.justice.digital.hmpps.personrecord.api.handler.vetting

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingMatchStatus.Companion.toVettingStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingName
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchData
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import java.util.SortedSet
import java.util.UUID

@Component
class VettingSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  fun search(vettingSearchRequest: VettingSearchRequest): VettingSearchResponse? {
    val personMatchScoresSorted = getPersonMatchScoresSortedByMatchProbabilityDescending(vettingSearchRequest)
    if (personMatchScoresSorted.isEmpty()) return null

    val strongestPersonsAcrossUniqueClusters = findStrongestPersonsAcrossUniqueClusters(personMatchScoresSorted)
    return constructSearchResult(strongestPersonsAcrossUniqueClusters)
  }

  private fun getPersonMatchScoresSortedByMatchProbabilityDescending(vettingSearchRequest: VettingSearchRequest): SortedSet<PersonMatchScore> = personMatchClient.vettingSearch(vettingSearchRequest)
    .toSortedSet { match1, match2 -> match2.candidateMatchProbability.compareTo(match1.candidateMatchProbability) }

  private fun findStrongestPersonsAcrossUniqueClusters(personMatchScoresSorted: SortedSet<PersonMatchScore>): Set<PersonEntity> {
    val strongestPersonsInClusters = mutableMapOf<Long, PersonEntity>()
    personMatchScoresSorted.forEach {
      val personEntity = personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!!
      if (strongestPersonsInClusters.contains(personEntity.personKey!!.id!!)) return@forEach
      strongestPersonsInClusters[personEntity.personKey!!.id!!] = personEntity
    }
    return strongestPersonsInClusters.values.toSet()
  }

  private fun constructSearchResult(personEntities: Set<PersonEntity>): VettingSearchResponse {
    val searchDataOrderedByMatchProbability = personEntities.map { personEntity ->
      val rootPersonData = toVettingSearchData(personEntity)
      val childPersonData = personEntity.personKey!!.personEntities
        .filter { it != personEntity }
        .map { toVettingSearchData(it) }
      rootPersonData.linkedRecords = childPersonData
      rootPersonData
    }
    return VettingSearchResponse(searchDataOrderedByMatchProbability)
  }

  fun toVettingSearchData(personEntity: PersonEntity): VettingSearchData {
    val mainPseudonym = personEntity.getPrimaryName()
    return VettingSearchData(
      name = VettingName(
        firstName = mainPseudonym.firstName,
        middleNames = mainPseudonym.middleNames,
        lastName = mainPseudonym.lastName,
        dateOfBirth = mainPseudonym.dateOfBirth,
      ),
      aliases = personEntity.getAliases().map { Alias.from(it) },
      addresses = personEntity.addresses.map { Address.from(it) },
      identifiers = personEntity.references.map { Reference.from(it) },
      sourceSystem = personEntity.sourceSystem,
      status = personEntity.personKey!!.status.toVettingStatus(),
    )
  }
}
