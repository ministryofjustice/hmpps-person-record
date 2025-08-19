package uk.gov.justice.digital.hmpps.personrecord.controller.probation

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ContactDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomProbationNationalityCode

class ProbationApiIntTest: WebTestBase() {

  @Test
  fun `should return ok for a create`() {
    val title = TitleCode.MR
    val firstName = randomName()
    val middleName = randomName()
    val lastName = randomName()

    val defendantId = randomDefendantId()
    val crn = randomCrn()
    val pnc = randomPnc()
    val cro = randomCro()

    val dateOfBirth = randomDate()

    val aliasFirstName = randomName()
    val aliasMiddleName = randomName()
    val aliasLastName = randomName()
    val aliasDateOfBirth = randomDate()

    val telephone = randomPhoneNumber()
    val mobile = randomPhoneNumber()
    val email = randomEmail()

    val noFixedAbode = false
    val startDate = randomDate()
    val endDate = randomDate()
    val postcode = randomPostcode()
    val fullAddress = randomFullAddress()

    val nationality = randomProbationNationalityCode()
    val ethnicity = randomEthnicity()

    val probationCase = ProbationCase(
      title = Value(value = title.name),
      name = Name(
        firstName = firstName,
        middleNames = middleName,
        lastName = lastName,
      ),
      identifiers = Identifiers(
        defendantId = defendantId,
        crn = crn,
        pnc = PNCIdentifier.from(pnc),
        cro = CROIdentifier.from(cro),
      ),
      dateOfBirth = dateOfBirth,
      aliases = listOf(
        ProbationCaseAlias(
          name = Name(
            aliasFirstName,
            aliasMiddleName,
            aliasLastName
          ),
          dateOfBirth = aliasDateOfBirth
        )
      ),
      contactDetails = ContactDetails(
        telephone = telephone,
        mobile = mobile,
        email = email,
      ),
      addresses = listOf(
        ProbationAddress(
          noFixedAbode = noFixedAbode,
          startDate = startDate,
          endDate = endDate,
          postcode = postcode,
          fullAddress = fullAddress,
        )
      ),
      nationality = Value(nationality),
      ethnicity = Value(ethnicity),
    )

    webTestClient.post()
      .uri(PROBATION_API_URL)
      .authorised(listOf(PROBATION_API_READ_WRITE))
      .bodyValue(probationCase)
      .exchange()
      .expectStatus()
      .isOk

  }

  companion object {
    private const val PROBATION_API_URL = "/person/delius"
  }
}