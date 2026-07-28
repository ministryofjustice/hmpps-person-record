package uk.gov.justice.digital.hmpps.personrecord.api.handler.vetting

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingName
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.Companion.toVettingStatus
import java.util.UUID

@Component
class VettingSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  fun search(vettingSearchRequest: VettingSearchRequest): VettingSearchResponse? {
    val matchScores = personMatchClient.vettingSearch(vettingSearchRequest)
    if (matchScores.isEmpty()) {
      return null
    }

    val strongestPersonMatchScore = matchScores.first()
    val personEntity = personRepository.findByMatchId(UUID.fromString(strongestPersonMatchScore.candidateMatchId))!!
    val mainPseudonym = personEntity.pseudonyms.first { it.nameType == NameType.PRIMARY }

    return VettingSearchResponse(
      name = VettingName(
        firstName = mainPseudonym.firstName,
        middleNames = mainPseudonym.middleNames,
        lastName = mainPseudonym.lastName,
        dateOfBirth = mainPseudonym.dateOfBirth,
      ),
      aliases = personEntity.pseudonyms.filter { it.nameType == NameType.ALIAS }.map { Alias.from(it) },
      addresses = personEntity.addresses.map { Address.from(it) },
      identifiers = CanonicalIdentifiers.from(personEntity), // TODO: this will include identifiers from other persons in the cluster? We have 'linkedRecords'?
      sourceSystem = personEntity.sourceSystem,
      status = personEntity.personKey!!.status.toVettingStatus(),
      linkedRecords = emptyList(),
    )
  }
}
