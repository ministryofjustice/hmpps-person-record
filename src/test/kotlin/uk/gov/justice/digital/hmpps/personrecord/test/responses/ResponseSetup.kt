package uk.gov.justice.digital.hmpps.personrecord.test.responses

import uk.gov.justice.digital.hmpps.personrecord.test.randomBirthCountry
import uk.gov.justice.digital.hmpps.personrecord.test.randomBirthPlace
import uk.gov.justice.digital.hmpps.personrecord.test.randomDateOfBirth
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationality
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomSex
import uk.gov.justice.digital.hmpps.personrecord.test.randomSexualOrientation
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
  val email: String? = randomEmail(),
  val dateOfBirth: LocalDate? = randomDateOfBirth(),
  val birthplace: String? = randomBirthPlace(),
  val birthCountry: String? = randomBirthCountry(),
  val nationality: String? = randomNationality(),
  val religion: String? = randomReligion(),
  val sexualOrientation: String? = randomSexualOrientation(),
  val sex: String? = randomSex(),
  val driverLicenseNumber: String? = randomDriverLicenseNumber(),

)
