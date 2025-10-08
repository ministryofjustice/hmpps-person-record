package uk.gov.justice.digital.hmpps.personrecord.test.responses

import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.EMAIL
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
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
  val secondaryNationality: String? = null,
  val religion: String? = null,
  val prisonNumber: String? = null,
  val ethnicity: String? = null,
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
) {
  companion object {

    fun from(person: Person): ApiResponseSetup = ApiResponseSetup(
      titleCode = person.titleCode?.name,
      firstName = person.firstName,
      middleName = person.middleNames,
      lastName = person.lastName,
      dateOfBirth = person.dateOfBirth,
      crn = person.crn,
      prisonNumber = person.prisonNumber,
      cro = person.references.getType(CRO).firstOrNull()?.identifierValue,
      pnc = person.references.getType(PNC).firstOrNull()?.identifierValue,
      aliases = person.aliases.map { ApiResponseSetupAlias(it.titleCode?.name, it.firstName, it.middleNames, it.lastName, it.dateOfBirth) },
      nationality = person.nationalities.firstOrNull()?.code?.name,
      secondaryNationality = person.nationalities.lastOrNull()?.code?.name,
      religion = person.religion,
      addresses = person.addresses.map { ApiResponseSetupAddress(it.noFixedAbode, it.startDate, it.endDate, it.postcode, it.fullAddress) },
      nationalInsuranceNumber = person.references.getType(NATIONAL_INSURANCE_NUMBER).firstOrNull()?.identifierValue,
      email = person.contacts.getType(EMAIL).firstOrNull()?.contactValue,
      driverLicenseNumber = person.references.getType(DRIVER_LICENSE_NUMBER).firstOrNull()?.identifierValue,
      identifiers = person.references.mapNotNull { ref -> ref.identifierValue?.let { ApiResponseSetupIdentifier(ref.identifierType.name, it) } },
      sentences = person.sentences.map { ApiResponseSetupSentences(it.sentenceDate) },
      gender = person.sexCode?.name,
    )
  }
}
