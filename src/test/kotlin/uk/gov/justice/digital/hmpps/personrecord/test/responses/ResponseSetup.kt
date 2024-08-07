package uk.gov.justice.digital.hmpps.personrecord.test.responses

import uk.gov.justice.digital.hmpps.personrecord.test.randomDateOfBirth
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.LocalDate

data class ApiResponseSetupIdentifier(val type: String, val value: String)

data class ApiResponseSetupAddress(val postcode: String, val fullAddress: String)

data class ApiResponseSetup(
  val crn: String? = null,
  val cro: String? = null,
  val pnc: String? = null,
  val prefix: String? = null,
  val nationality: String? = null,
  val religion: String? = null,
  val prisonNumber: String,
  val addresses: List<ApiResponseSetupAddress> = listOf(ApiResponseSetupAddress(randomPostcode(), randomFullAddress())),
  val nationalInsuranceNumber: String? = null,
  val email: String? = randomEmail(),
  val dateOfBirth: LocalDate? = randomDateOfBirth(),
  val driverLicenseNumber: String? = randomDriverLicenseNumber(),
  val identifiers: List<ApiResponseSetupIdentifier> = listOf(),

)
