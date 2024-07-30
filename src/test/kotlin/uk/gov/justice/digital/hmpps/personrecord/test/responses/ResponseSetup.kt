package uk.gov.justice.digital.hmpps.personrecord.test.responses

import uk.gov.justice.digital.hmpps.personrecord.test.randomDateOfBirth
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationality
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import java.time.LocalDate

data class ApiResponseSetupAddress(val postcode: String)

data class ApiResponseSetup(
  val crn: String? = null,
  val cro: String? = null,
  val pnc: String? = null,
  val prefix: String? = null,
  val prisonNumber: String,
  val addresses: List<ApiResponseSetupAddress> = listOf(ApiResponseSetupAddress(randomPostcode())),
  val nationalInsuranceNumber: String? = null,
  val driverLicenseNumber: String? = null,
  val email: String? = randomEmail(),
  val dateOfBirth: LocalDate? = randomDateOfBirth(),
  val religion: String? = randomReligion(),
  val nationality: String? = randomNationality(),
)
