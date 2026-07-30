package uk.gov.justice.digital.hmpps.personrecord.api.handler.vetting

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingMatchStatus.Companion.toVettingStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingName
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchData
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import java.util.UUID

@Component
class VettingSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  fun search(vettingSearchRequest: VettingSearchRequest): VettingSearchResponse? {
    val personMatchScoresSorted = personMatchClient.vettingSearch(vettingSearchRequest).sortedByDescending { it.candidateMatchProbability }
    if (personMatchScoresSorted.isEmpty()) return null

    val searchDataOrderedByMatchProbability = mutableListOf<VettingSearchData>()
    val completedClustersByClusterId = mutableMapOf<Long, Boolean>()
    personMatchScoresSorted.forEach { personMatchScore ->
      val personEntity = personRepository.findByMatchId(UUID.fromString(personMatchScore.candidateMatchId))!!
      val clusterId = personEntity.personKey!!.id!!
      if (completedClustersByClusterId.contains(clusterId)) return@forEach

      val rootPersonData = toVettingSearchData(personEntity)

      val childPersonData = personEntity.personKey!!.personEntities
        .filter { it != personEntity }
        .map { toVettingSearchData(it) }
      rootPersonData.linkedRecords = childPersonData
      searchDataOrderedByMatchProbability.add(rootPersonData)
      completedClustersByClusterId[clusterId] = true
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
