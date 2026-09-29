package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupContact

class PrisonEventListenerFeatureFlagTest : MessagingTestBase() {

  // TODO delete this test when we switch PrisonPersonServiceProxy to ignore addresses and contacts in prod
  @ActiveProfiles("prod")
  @Nested
  inner class Prod {
    @Test
    fun `should continue to save addresses and contacts when person level data is created`() {
      val prisonNumber = randomPrisonNumber()

      val updatedAddressResponse = ApiResponseSetupAddress(postcode = randomPostcode())
      val updatedContactResponse = ApiResponseSetupContact(
        value = randomPhoneNumber(),
        type = ContactType.HOME,
      )

      stubNoMatchesPersonMatch()
      stubPersonMatchUpsert()
      prisonUpdateEventAndResponseSetup(
        ApiResponseSetup(
          prisonNumber = prisonNumber,
          firstName = randomName(),
          addresses = listOf(updatedAddressResponse),
          contacts = listOf(updatedContactResponse),
        ),
      )

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
      val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(actualPersonEntity.addresses.find { it.postcode == updatedAddressResponse.postcode }).isNotNull()
      assertThat(actualPersonEntity.contacts.find { it.contactValue == updatedContactResponse.value }).isNotNull()
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

  // TODO delete this test when we switch PrisonPersonServiceProxy to ignore addresses and contacts in preprod
  @ActiveProfiles("preprod")
  @Nested
  inner class PreProd {
    @Test
    fun `should continue to save addresses and contacts when person level data is created`() {
      val prisonNumber = randomPrisonNumber()

      val updatedAddressResponse = ApiResponseSetupAddress(postcode = randomPostcode())
      val updatedContactResponse = ApiResponseSetupContact(
        value = randomPhoneNumber(),
        type = ContactType.HOME,
      )

      stubNoMatchesPersonMatch()
      stubPersonMatchUpsert()
      prisonUpdateEventAndResponseSetup(
        ApiResponseSetup(
          prisonNumber = prisonNumber,
          firstName = randomName(),
          addresses = listOf(updatedAddressResponse),
          contacts = listOf(updatedContactResponse),
        ),
      )

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to "NOMIS", "PRISON_NUMBER" to prisonNumber))
      val actualPersonEntity = personRepository.findByPrisonNumber(prisonNumber)!!
      assertThat(actualPersonEntity.addresses.find { it.postcode == updatedAddressResponse.postcode }).isNotNull()
      assertThat(actualPersonEntity.contacts.find { it.contactValue == updatedContactResponse.value }).isNotNull()
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
}
