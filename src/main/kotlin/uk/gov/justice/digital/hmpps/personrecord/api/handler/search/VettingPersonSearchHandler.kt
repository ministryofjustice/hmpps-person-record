package uk.gov.justice.digital.hmpps.personrecord.api.handler.search

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchData
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingPersonSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.VettingResult
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchScore
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
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

  private fun findClusters(searchResults: List<PersonMatchScore>): List<PersonKeyEntity> = searchResults.map {
    personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!!.personKey!!
  }.distinctBy { it.personUUID }

  private fun constructResponse(clusters: List<PersonKeyEntity>): VettingPersonSearchResponse {
    val results = clusters.mapNotNull { cluster ->
      VettingResult(
        cluster.personEntities
          .filterNot { it.isCourtRecord() }
          .map { SearchData.from(it) },
      ).takeIf { it.results.isNotEmpty() }
    }
    return VettingPersonSearchResponse(results)
  }
}

private fun PersonEntity.isCourtRecord(): Boolean = this.sourceSystem in listOf(COMMON_PLATFORM, LIBRA)
