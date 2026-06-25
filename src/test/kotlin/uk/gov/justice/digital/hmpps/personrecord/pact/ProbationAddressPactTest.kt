package uk.gov.justice.digital.hmpps.personrecord.pact

import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.junitsupport.loader.PactBroker
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressUsageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.ZonedDateTime
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Provider("core-person-record")
@VerificationReports(value = ["markdown", "console"], reportDir = "build/pact")
@PactBroker
class ProbationAddressPactTest : E2ETestBase() {

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider::class)
  fun template(context: PactVerificationContext, request: HttpRequest) {
    val token = jwtAuthorisationHelper.createJwtAccessToken(roles = listOf(API_READ_ONLY))
    request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    context.verifyInteraction()
  }

  @State("An address exists for CRN and address ID")
  fun setupAddressState(): Map<String, String> {
    addressRepository.deleteAllInBatch()
    personRepository.deleteAllInBatch()
    val crn = "X744208"
    val personEntity = PersonEntity(
      sourceSystem = SourceSystemType.COMMON_PLATFORM,
      matchId = UUID.randomUUID(),
    ).apply {
      this.crn = crn
    }
    personRepository.save(personEntity)

    val addressEntity = AddressEntity().apply {
      this.person = personEntity
      this.noFixedAbode = false
      this.startDate = ZonedDateTime.parse("2020-02-26T00:00:00Z")
      this.endDate = ZonedDateTime.parse("2023-07-15T00:00:00Z")
      this.postcode = "SW1H 9AJ"
      this.uprn = "100128991537"
      this.subBuildingName = "Sub building 2"
      this.buildingName = "Main Building"
      this.buildingNumber = "102"
      this.thoroughfareName = "Petty France"
      this.dependentLocality = "Westminster"
      this.postTown = "London"
      this.county = "Greater London"
      this.countryCode = CountryCode.GBR
      this.comment = "Some comment"
      this.statusCode = AddressStatusCode.M
      this.isVerified = true
    }
    val savedAddress = addressRepository.saveAndFlush(addressEntity)
    val addresses = addressRepository.findAll().first()

    val usageEntity = AddressUsageEntity(
      usageCode = AddressUsageCode.CURFEW,
      active = true,
    ).apply {
      this.address = savedAddress
    }

    val contactEntity = ContactEntity(
      extension = "1234",
      contactType = ContactType.HOME,
      contactValue = "+44 20 7946 0000",
    ).apply {
      this.address = savedAddress
    }
    savedAddress.usages = mutableListOf(usageEntity)
    savedAddress.contacts = mutableListOf(contactEntity)
    addressRepository.saveAndFlush(savedAddress)

    return mapOf(
      "crn" to "X744208",
      "cprAddressId" to addresses.updateId.toString(),
    )
  }
}
