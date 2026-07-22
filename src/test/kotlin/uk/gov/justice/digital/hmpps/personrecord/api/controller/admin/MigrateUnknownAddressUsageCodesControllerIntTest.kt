package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDeliusAddressId
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationAddress

class MigrateUnknownAddressUsageCodesControllerIntTest : WebTestBase() {

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
          usages = listOf(AddressUsage(AddressUsageCode.UNKNOWN, randomBoolean()))
        )
      )
    )

    val usageCode = randomAddressUsageCode()

    stubGetAddressRequestToProbation(
      ProbationAddress(
        postcode = postcode,
        deliusAddressId = deliusAddressId,
        usage = ProbationAddressUsage(
          code = usageCode.name,
          description = usageCode.description
        )
      )
    )

    sendPostRequestAsserted<String>(
      url = "/admin/migrate-unknown-address-usage-codes",
      body = "",
      expectedStatus = HttpStatus.OK,
      roles = listOf("ROLE_ADMIN"),
    ).returnResult().responseBody!!

    awaitAssert {
      assertThat(addressRepository.findByDeliusAddressId(deliusAddressId)?.usages?.first()?.usageCode).isEqualTo(usageCode)
    }
  }

  fun stubGetAddressRequestToProbation(probationAddress: ProbationAddress, status: Int = 200) {
    stubGetRequest(
      url = "/address/${probationAddress.deliusAddressId}",
      status = status,
      body = probationAddress(
        address = ApiResponseSetupAddress(
          noFixedAbode = probationAddress.noFixedAbode,
          startDateTime = probationAddress.startDateTime,
          endDateTime = probationAddress.endDateTime,
          postcode = probationAddress.postcode,
          fullAddress = probationAddress.fullAddress,
          buildingName = probationAddress.buildingName,
          addressNumber = probationAddress.addressNumber,
          streetName = probationAddress.streetName,
          district = probationAddress.district,
          townCity = probationAddress.townCity,
          county = probationAddress.county,
          deliusAddressId = probationAddress.deliusAddressId!!,
          isVerified = probationAddress.isVerified,
          status = ApiResponseSetupAddressStatus(probationAddress.status?.code, probationAddress.status?.description),
          usage = ApiResponseSetupAddressUsage(probationAddress.usage?.code, probationAddress.usage?.description),
          uprn = probationAddress.uprn,
          notes = probationAddress.notes,
          telephoneNumber = probationAddress.telephoneNumber,
        ),
      ),
    )
  }
}