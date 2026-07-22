package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode.UNKNOWN
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDeliusAddressId
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationAddress

class MigrateUnknownAddressUsageCodesControllerIntTest : WebTestBase() {

  @BeforeEach
  fun cleanup() {
    deleteAllPersonData()
  }

  @Nested
  inner class ErrorRecovery {
    @Test
    fun `should retry on HTTP error`() {
      val probationCase = createRandomProbationCase()
      val deliusAddressId = randomDeliusAddressId()
      val postcode = randomPostcode()
      createPersonWithNewKey(
        Person.from(probationCase),
        configure = addAddressToRecord(
          Address(
            postcode = postcode,
            deliusAddressId = deliusAddressId,
            usages = listOf(AddressUsage(UNKNOWN, randomBoolean())),
          ),
        ),
      )

      val usageCode = randomAddressUsageCode()

      val probationAddress1 = ProbationAddress(
        postcode = postcode,
        deliusAddressId = deliusAddressId,
        usage = ProbationAddressUsage(
          code = usageCode.name,
          description = usageCode.description,
        ),
      )
      stubGetRequest(
        url = "/address/${probationAddress1.deliusAddressId}",
        status = 500,
        body = "",
        scenarioName = "Retry when failed",
        nextScenarioState = "Request will Pass",

      )

      stubGetRequest(
        url = "/address/${probationAddress1.deliusAddressId}",
        scenarioName = "Retry when failed",
        currentScenarioState = "Request will Pass",
        body = probationAddress(
          address = ApiResponseSetupAddress(
            noFixedAbode = probationAddress1.noFixedAbode,
            startDateTime = probationAddress1.startDateTime,
            endDateTime = probationAddress1.endDateTime,
            postcode = probationAddress1.postcode,
            fullAddress = probationAddress1.fullAddress,
            buildingName = probationAddress1.buildingName,
            addressNumber = probationAddress1.addressNumber,
            streetName = probationAddress1.streetName,
            district = probationAddress1.district,
            townCity = probationAddress1.townCity,
            county = probationAddress1.county,
            deliusAddressId = probationAddress1.deliusAddressId!!,
            isVerified = probationAddress1.isVerified,
            status = ApiResponseSetupAddressStatus(
              probationAddress1.status?.code,
              probationAddress1.status?.description,
            ),
            usage = ApiResponseSetupAddressUsage(probationAddress1.usage?.code, probationAddress1.usage?.description),
            uprn = probationAddress1.uprn,
            notes = probationAddress1.notes,
            telephoneNumber = probationAddress1.telephoneNumber,
          ),
        ),
      )

      sendPostRequestAsserted<String>(
        url = "/admin/migrate-unknown-address-usage-codes",
        body = "",
        expectedStatus = OK,
        sendAuthorised = false,
        roles = listOf(),
      ).returnResult().responseBody!!

      awaitAssert {
        assertThat(addressRepository.findByDeliusAddressId(deliusAddressId)?.usages?.first()?.usageCode).isEqualTo(
          usageCode,
        )
      }
    }
  }

  @Nested
  inner class Success {

    @Test
    fun `should populate unknown address usage codes`() {
      val probationCase = createRandomProbationCase()
      val deliusAddressId = randomDeliusAddressId()
      val postcode = randomPostcode()
      createPersonWithNewKey(
        Person.from(probationCase),
        configure = addAddressToRecord(
          Address(
            postcode = postcode,
            deliusAddressId = deliusAddressId,
            usages = listOf(AddressUsage(UNKNOWN, randomBoolean())),
          ),
        ),
      )

      val usageCode = randomAddressUsageCode()

      val probationAddress1 = ProbationAddress(
        postcode = postcode,
        deliusAddressId = deliusAddressId,
        usage = ProbationAddressUsage(
          code = usageCode.name,
          description = usageCode.description,
        ),
      )
      stubGetRequest(
        url = "/address/${probationAddress1.deliusAddressId}",
        body = probationAddress(
          address = ApiResponseSetupAddress(
            noFixedAbode = probationAddress1.noFixedAbode,
            startDateTime = probationAddress1.startDateTime,
            endDateTime = probationAddress1.endDateTime,
            postcode = probationAddress1.postcode,
            fullAddress = probationAddress1.fullAddress,
            buildingName = probationAddress1.buildingName,
            addressNumber = probationAddress1.addressNumber,
            streetName = probationAddress1.streetName,
            district = probationAddress1.district,
            townCity = probationAddress1.townCity,
            county = probationAddress1.county,
            deliusAddressId = probationAddress1.deliusAddressId!!,
            isVerified = probationAddress1.isVerified,
            status = ApiResponseSetupAddressStatus(probationAddress1.status?.code, probationAddress1.status?.description),
            usage = ApiResponseSetupAddressUsage(probationAddress1.usage?.code, probationAddress1.usage?.description),
            uprn = probationAddress1.uprn,
            notes = probationAddress1.notes,
            telephoneNumber = probationAddress1.telephoneNumber,
          ),
        ),
      )

      sendPostRequestAsserted<String>(
        url = "/admin/migrate-unknown-address-usage-codes",
        body = "",
        expectedStatus = OK,
        sendAuthorised = false,
        roles = listOf(),
      ).returnResult().responseBody!!

      awaitAssert {
        assertThat(addressRepository.findByDeliusAddressId(deliusAddressId)?.usages?.first()?.usageCode).isEqualTo(
          usageCode,
        )
      }
    }
  }
}
