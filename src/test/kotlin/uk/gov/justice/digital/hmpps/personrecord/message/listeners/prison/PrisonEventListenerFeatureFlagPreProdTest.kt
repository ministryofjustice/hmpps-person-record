package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias

@ActiveProfiles("preprod")
class PrisonEventListenerFeatureFlagPreProdTest : MessagingTestBase() {

  @Test
  fun `should continue to save aliases when person level data is created`() {
    val prisonNumber = randomPrisonNumber()

    val updatedFirstName = randomName()
    val ethnicity = randomPrisonEthnicity()
    val updatedNationality = randomPrisonNationalityCode()
    val title = randomTitleCode()
    val updatedSexCode = randomPrisonSexCode()

    val updatedAliasGender = randomPrisonSexCode()
    val updatedAlias = ApiResponseSetupAlias(title = title.key, firstName = randomName(), lastName = randomName(), gender = updatedAliasGender.key)

    stubNoMatchesPersonMatch()
    stubPersonMatchUpsert()
    prisonUpdateEventAndResponseSetup(ApiResponseSetup(gender = updatedSexCode.key, title = title.key, prisonNumber = prisonNumber, firstName = updatedFirstName, nationality = updatedNationality, ethnicity = ethnicity, aliases = listOf(updatedAlias)))

    checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
    val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber)!!
    assertThat(actualPersonEntity.pseudonyms.size).isEqualTo(2)
  }

  private fun prisonUpdateEventAndResponseSetup(apiResponseSetup: ApiResponseSetup) {
    stubPrisonResponse(apiResponseSetup)

    publishDomainEvent(
      PrisonPersonUpdated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", apiResponseSetup.prisonNumber!!))),
      ),
    )
  }
}
