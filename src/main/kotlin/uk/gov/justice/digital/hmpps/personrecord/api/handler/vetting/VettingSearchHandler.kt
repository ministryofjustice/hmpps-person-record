package uk.gov.justice.digital.hmpps.personrecord.api.handler.vetting

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingMatchStatus.Companion.toVettingStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingName
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
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

    val matchingPersonEntities = matchScores.map { personRepository.findByMatchId(UUID.fromString(it.candidateMatchId))!! }
    val strongestMatchingPerson = matchingPersonEntities.first { it.matchId.toString() == matchScores.first().candidateMatchId }
    val weakerMatchingPersons = matchingPersonEntities.filter { it != strongestMatchingPerson }

    val rootSearchResponse = toVettingSearchResponse(strongestMatchingPerson)
    val linkedRecords = weakerMatchingPersons.map { toVettingSearchResponse(it) }
    rootSearchResponse.linkedRecords = linkedRecords
    return rootSearchResponse
  }

  fun toVettingSearchResponse(personEntity: PersonEntity): VettingSearchResponse {
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
      identifiers = personEntity.references.map { Reference.from(it) },
      sourceSystem = personEntity.sourceSystem,
      status = personEntity.personKey!!.status.toVettingStatus(),
    )
  }
}
