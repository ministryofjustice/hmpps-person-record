package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junitsupport.State
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber

class CourtDataIngestionProviderPactTest : AbstractProviderPactTests() {
  @State("A person exists for the requested common platform Id")
  fun aPersonExistsForTheRequestedCommonPlatformId(): Map<String, String> = createCourtDataIngestionPerson()

  @State("A person exists for the requested prisoner number")
  fun aPersonExistsForTheRequestedPrisonerNumber(): Map<String, String> = createCourtDataIngestionPerson()

  private fun createCourtDataIngestionPerson(): Map<String, String> {
    val defendantId = randomDefendantId()
    val prisonerNumber = randomPrisonNumber()

    createPersonKey()
      .addPerson(createRandomCommonPlatformPersonDetails(defendantId))
      .addPerson(createRandomPrisonPersonDetails(prisonerNumber))

    return mapOf(
      "defendantId" to defendantId,
      "prisonerNumber" to prisonerNumber,
    )
  }
}
