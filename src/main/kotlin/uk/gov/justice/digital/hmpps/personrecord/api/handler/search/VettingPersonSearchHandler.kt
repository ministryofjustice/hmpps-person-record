package uk.gov.justice.digital.hmpps.personrecord.api.handler.search

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchData
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingResult
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.util.UUID

@Component
class VettingPersonSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  fun search(personSearchRequest: VettingPersonSearchRequest): VettingPersonSearchResponse {
    val personMatchScores = personMatchClient.search(PersonMatchSearchRequest.from(personSearchRequest))
    val clusters = findClusters(personMatchScores)
    return constructResponse(clusters)
  }

  private fun findClusters(personSearchRequest: List<PersonMatchScore>): List<PersonKeyEntity> {
    val clusters = mutableMapOf<Long, PersonKeyEntity>()
    personSearchRequest.forEach {
      val personEntity = personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!!
      val clusterId = personEntity.personKey!!.id!!
      if (!clusters.containsKey(clusterId)) {
        clusters[clusterId] = personEntity.personKey!!
      }
    }
    return clusters.values.toList()
  }

  private fun constructResponse(clusters: List<PersonKeyEntity>): VettingPersonSearchResponse {
    val results = clusters.map { cluster ->
      VettingResult(cluster.personEntities.map { SearchData.from(it) })
    }
    return VettingPersonSearchResponse(results)
  }
}
