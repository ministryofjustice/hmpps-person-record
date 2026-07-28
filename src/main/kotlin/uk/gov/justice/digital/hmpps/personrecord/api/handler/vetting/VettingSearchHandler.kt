package uk.gov.justice.digital.hmpps.personrecord.api.handler.vetting

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingName
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.vetting.VettingSearchResponse
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class VettingSearchHandler(
  private val personRepository: PersonRepository,
  private val personMatchClient: PersonMatchClient,
) {

  fun search(vettingSearchRequest: VettingSearchRequest): VettingSearchResponse {
    val matchScores = personMatchClient.vettingSearch(vettingSearchRequest)

    return VettingSearchResponse(
      name = VettingName(
        firstName = TODO(),
        middleNames = TODO(),
        lastName = TODO(),
        dateOfBirth = TODO(),
      ),
      aliases = TODO(),
      addresses = TODO(),
      identifiers = TODO(),
      sourceSystem = TODO(),
      status = TODO(),
      linkedRecords = TODO(),
    )
  }
}
