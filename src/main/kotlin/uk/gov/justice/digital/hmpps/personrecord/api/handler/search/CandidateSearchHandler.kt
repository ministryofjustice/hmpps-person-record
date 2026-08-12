package uk.gov.justice.digital.hmpps.personrecord.api.handler.search

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.util.UUID

@Component
class CandidateSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  fun searchByDefendantId(defendantId: String): List<CanonicalRecord> = personRepository.findByDefendantId(defendantId)
    ?.let { getCandidateMatches(it.matchId) } ?: emptyList()

  private fun getCandidateMatches(matchId: UUID): List<CanonicalRecord> = getCandidateMatchScoresSortedByMatchWeightDescending(matchId)
    .map { personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!! }
    .filter { it.sourceSystem == SourceSystemType.DELIUS }
    .map { CanonicalRecord.from(it) }

  private fun getCandidateMatchScoresSortedByMatchWeightDescending(matchId: UUID) = personMatchClient.searchCandidates(matchId).sortedByDescending { it.candidateMatchWeight }
}
