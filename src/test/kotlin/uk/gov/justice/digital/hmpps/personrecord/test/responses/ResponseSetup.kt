package uk.gov.justice.digital.hmpps.personrecord.test.responses

import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.LocalDate

data class ApiResponseSetupIdentifier(val type: String, val value: String)

data class ApiResponseSetupAddress(
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val postcode: String?,
  val fullAddress: String?,
)

data class ApiResponseSetupSentences(val sentenceDate: LocalDate?)

data class ApiResponseSetupAlias(val title: String? = null, val firstName: String? = null, val middleName: String? = null, val lastName: String? = null, val dateOfBirth: LocalDate? = null)

data class ApiResponseSetup(
  val title: String? = null,
  val titleCode: String? = null,
  val crn: String? = null,
  val cro: String? = null,
  val pnc: String? = null,
  val firstName: String? = randomName(),
  val middleName: String? = randomName(),
  val lastName: String? = randomName(),
  val aliases: List<ApiResponseSetupAlias> = listOf(ApiResponseSetupAlias(firstName = randomName(), middleName = randomName(), lastName = randomName(), dateOfBirth = randomDate())),
  val nationality: String? = null,
  val religion: String? = null,
  val prisonNumber: String? = null,
  val ethnicity: String? = null,
  val ethnicityCode: EthnicityCode? = null,
  val addresses: List<ApiResponseSetupAddress> = listOf(ApiResponseSetupAddress(postcode = randomPostcode(), fullAddress = randomFullAddress())),
  val nationalInsuranceNumber: String? = null,
  val email: String? = randomEmail(),
  val dateOfBirth: LocalDate? = randomDate(),
  val driverLicenseNumber: String? = randomDriverLicenseNumber(),
  val identifiers: List<ApiResponseSetupIdentifier> = listOf(),
  val sentences: List<ApiResponseSetupSentences>? = listOf(),
  val sentenceStartDate: LocalDate? = null,
  val primarySentence: Boolean? = null,
  val gender: String? = null,
)
