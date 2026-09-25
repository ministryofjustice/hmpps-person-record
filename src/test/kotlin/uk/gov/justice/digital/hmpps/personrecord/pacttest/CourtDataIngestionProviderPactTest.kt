package uk.gov.justice.digital.hmpps.personrecord.pacttest

import au.com.dius.pact.provider.junitsupport.State

class CourtDataIngestionProviderPactTest : AbstractProviderPactTests() {
  @State("A person exists for the requested common platform Id")
  fun aPersonExistsForTheRequestedCommonPlatformId(): Map<String, String> {
    createCourtDataIngestionPerson()
    return mapOf("defendantId" to COMMON_PLATFORM_ID)
  }

  @State("A person exists for the requested prisoner number")
  fun aPersonExistsForTheRequestedPrisonerNumber(): Map<String, String> {
    createCourtDataIngestionPerson()
    return mapOf("prisonerNumber" to PRISON_NUMBER)
  }

  private fun createCourtDataIngestionPerson() {
    deleteAllPersonData()
    createPersonKey()
      .addPerson(createRandomCommonPlatformPersonDetails(COMMON_PLATFORM_ID))
      .addPerson(createRandomPrisonPersonDetails(PRISON_NUMBER))
  }

  private companion object {
    const val COMMON_PLATFORM_ID = "08d5d16c-de97-49f1-a10b-fdf6d1986843"
    const val PRISON_NUMBER = "OFF900"
  }
}
