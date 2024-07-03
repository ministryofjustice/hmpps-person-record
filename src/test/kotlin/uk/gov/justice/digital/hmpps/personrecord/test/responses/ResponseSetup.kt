package uk.gov.justice.digital.hmpps.personrecord.test.responses

import java.time.LocalDate

data class ApiResponseSetupAddress(val postcode: String)

data class ApiResponseSetup(
  val crn: String? = null,
  val cro: String? = null,
  val pnc: String? = null,
  val prefix: String? = null,
  val prisonNumber: String,
  val addresses: List<ApiResponseSetupAddress>,
  val nationalInsuranceNumber: String? = null,
  val email: String? = null,
  val dateOfBirth: LocalDate? = null,
)
