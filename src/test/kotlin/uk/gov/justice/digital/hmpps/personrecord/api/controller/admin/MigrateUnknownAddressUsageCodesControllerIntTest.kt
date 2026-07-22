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
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
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
    fun `should bypass address update of 404 error`() {
      val probationCase = createRandomProbationCase()
      val deliusAddressId1 = randomDeliusAddressId()
      val deliusAddressId2 = randomDeliusAddressId()
      val postcode = randomPostcode()
      createPersonWithNewKey(
        Person.from(probationCase),
        configure = addAddressToRecord(Address(postcode = postcode, deliusAddressId = deliusAddressId1, usages = listOf(AddressUsage(UNKNOWN, randomBoolean()))))
      )
      createPersonWithNewKey(
        Person.from(probationCase).copy(crn = randomCrn()),
        configure = addAddressToRecord(Address(postcode = postcode, deliusAddressId = deliusAddressId2, usages = listOf(AddressUsage(UNKNOWN, randomBoolean()))))
      )

      val usageCode = randomAddressUsageCode()

      val probationAddress1 = ProbationAddress(
        postcode = postcode,
        deliusAddressId = deliusAddressId1,
        usage = ProbationAddressUsage(
          code = usageCode.name,
          description = usageCode.description,
        ),
      )

      val probationAddress2 = ProbationAddress(
        postcode = postcode,
        deliusAddressId = deliusAddressId2,
        usage = ProbationAddressUsage(
          code = usageCode.name,
          description = usageCode.description,
        ),
      )

      stubGetRequest(
        url = "/address/${probationAddress1.deliusAddressId}",
        status = 404,
        body = "",
        scenarioName = "Skip 404 and continue with next address",
        nextScenarioState = "Request will Pass",
      )

      stubGetRequest(
        url = "/address/${probationAddress2.deliusAddressId}",
        scenarioName = "Skip 404 and continue with next address",
        currentScenarioState = "Request will Pass",
        body = probationAddress(
          address = ApiResponseSetupAddress(
            noFixedAbode = probationAddress2.noFixedAbode,
            startDateTime = probationAddress2.startDateTime,
            endDateTime = probationAddress2.endDateTime,
            postcode = probationAddress2.postcode,
            fullAddress = probationAddress2.fullAddress,
            buildingName = probationAddress2.buildingName,
            addressNumber = probationAddress2.addressNumber,
            streetName = probationAddress2.streetName,
            district = probationAddress2.district,
            townCity = probationAddress2.townCity,
            county = probationAddress2.county,
            deliusAddressId = probationAddress2.deliusAddressId!!,
            isVerified = probationAddress2.isVerified,
            status = ApiResponseSetupAddressStatus(
              probationAddress2.status?.code,
              probationAddress2.status?.description,
            ),
            usage = ApiResponseSetupAddressUsage(probationAddress2.usage?.code, probationAddress2.usage?.description),
            uprn = probationAddress2.uprn,
            notes = probationAddress2.notes,
            telephoneNumber = probationAddress2.telephoneNumber,
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
        assertThat(addressRepository.findByDeliusAddressId(deliusAddressId1)?.usages?.first()?.usageCode).isEqualTo(UNKNOWN)
        assertThat(addressRepository.findByDeliusAddressId(deliusAddressId2)?.usages?.first()?.usageCode).isEqualTo(usageCode)
      }
    }

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
