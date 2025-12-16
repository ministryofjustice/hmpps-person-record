package uk.gov.justice.digital.hmpps.personrecord.test.responses

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType.EMAIL
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
data class ApiResponseSetupAdditionalIdentifier(val type: String, val value: String)

data class ApiResponseSetupContact(val type: ContactType, val value: String)

data class ApiResponseSetupAddress(
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val postcode: String?,
  val fullAddress: String? = null,
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val district: String? = null,
  val townCity: String? = null,
  val county: String? = null,
  val uprn: String? = null,
  val notes: String? = null,
  val telephoneNumber: String? = null,
)

data class ApiResponseSetupSentences(val sentenceDate: LocalDate?)

data class ApiResponseSetupAlias(val title: String? = null, val firstName: String? = null, val middleName: String? = null, val lastName: String? = null, val dateOfBirth: LocalDate? = null, val gender: String? = null)

data class ApiResponseSetup(
  val title: String? = null,
  val crn: String? = null,
  val cro: String? = null,
  val pnc: String? = null,
  val firstName: String? = randomName(),
  val middleName: String? = randomName(),
  val lastName: String? = randomName(),
  val aliases: List<ApiResponseSetupAlias> = listOf(ApiResponseSetupAlias(firstName = randomName(), middleName = randomName(), lastName = randomName(), dateOfBirth = randomDate())),
  val nationality: String? = null,
  val secondNationality: String? = null,
  val religion: String? = null,
  val prisonNumber: String? = null,
  val ethnicity: String? = null,
  val addresses: List<ApiResponseSetupAddress> = listOf(ApiResponseSetupAddress(postcode = randomPostcode(), fullAddress = randomFullAddress())),
  val nationalInsuranceNumber: String? = null,
  val email: String? = randomEmail(),
  val dateOfBirth: LocalDate? = randomDate(),
  val dateOfDeath: LocalDate? = null,
  val driverLicenseNumber: String? = randomDriverLicenseNumber(),
  val identifiers: List<ApiResponseSetupIdentifier> = listOf(),
  val additionalIdentifiers: List<ApiResponseSetupAdditionalIdentifier> = listOf(),
  val sentences: List<ApiResponseSetupSentences>? = listOf(),
  val sentenceStartDate: LocalDate? = null,
  val primarySentence: Boolean? = null,
  val gender: String? = null,
  val genderIdentity: String? = null,
  val selfDescribedGenderIdentity: String? = null,
  val sexualOrientation: String? = null,
  val contacts: List<ApiResponseSetupContact> = listOf(),
) {
  companion object {
    fun from(probationCase: ProbationCase): ApiResponseSetup = ApiResponseSetup(
      crn = probationCase.identifiers.crn,
      pnc = probationCase.identifiers.pnc,
      title = probationCase.title?.value,
      firstName = probationCase.name.firstName,
      middleName = probationCase.name.middleNames,
      lastName = probationCase.name.lastName,
      dateOfBirth = probationCase.dateOfBirth,
      cro = probationCase.identifiers.cro,
      aliases = probationCase.aliases?.map { ApiResponseSetupAlias(firstName = it.name.firstName, middleName = it.name.middleNames, lastName = it.name.lastName, dateOfBirth = it.dateOfBirth, gender = it.gender?.value) } ?: emptyList(),
      nationality = probationCase.nationality?.value,
      secondNationality = probationCase.secondNationality?.value,
      religion = probationCase.religion?.value,
      addresses = probationCase.addresses.map { ApiResponseSetupAddress(it.noFixedAbode, it.startDate, it.endDate, it.postcode, it.fullAddress) }, //TODO telephone number
      nationalInsuranceNumber = probationCase.identifiers.nationalInsuranceNumber,
      email = probationCase.contactDetails?.email,
      driverLicenseNumber = probationCase.identifiers.additionalIdentifiers?.firstOrNull { it.type?.value == DRIVER_LICENSE_NUMBER.name }?.value,
      additionalIdentifiers = probationCase.identifiers.additionalIdentifiers?.mapNotNull { ref -> ref.value?.let { ApiResponseSetupAdditionalIdentifier(ref.type?.value!!, ref.value) } } ?: emptyList(),
      sentences = probationCase.sentences?.map { ApiResponseSetupSentences(it.sentenceDate) },
      gender = probationCase.gender?.value,
      ethnicity = probationCase.ethnicity?.value,
      sexualOrientation = probationCase.sexualOrientation?.value,
      genderIdentity = probationCase.genderIdentity?.value,
      selfDescribedGenderIdentity = probationCase.selfDescribedGenderIdentity,
      // TODO contacts

    )

    // this is a bad idea - should be done differently for probation and prison
    fun from(person: Person): ApiResponseSetup = ApiResponseSetup(
      title = person.titleCode?.name,
      firstName = person.firstName,
      middleName = person.middleNames,
      lastName = person.lastName,
      dateOfBirth = person.dateOfBirth,
      crn = person.crn,
      prisonNumber = person.prisonNumber,
      cro = person.references.getType(CRO).firstOrNull()?.identifierValue,
      pnc = person.references.getType(PNC).firstOrNull()?.identifierValue,
      aliases = person.aliases.map { ApiResponseSetupAlias(it.titleCode?.name, it.firstName, it.middleNames, it.lastName, it.dateOfBirth) },
      nationality = person.nationalities.firstOrNull()?.name,
      secondNationality = when {
        person.nationalities.size > 1 -> person.nationalities.lastOrNull()?.name else -> null
      },
      religion = person.religion,
      addresses = person.addresses.map { ApiResponseSetupAddress(it.noFixedAbode, it.startDate, it.endDate, it.postcode, it.fullAddress) },
      nationalInsuranceNumber = person.references.getType(NATIONAL_INSURANCE_NUMBER).firstOrNull()?.identifierValue,
      email = person.contacts.getType(EMAIL).firstOrNull()?.contactValue,
      driverLicenseNumber = person.references.getType(DRIVER_LICENSE_NUMBER).firstOrNull()?.identifierValue,
      identifiers = person.references.mapNotNull { ref -> ref.identifierValue?.let { ApiResponseSetupIdentifier(ref.identifierType.name, it) } },
      sentences = person.sentences.map { ApiResponseSetupSentences(it.sentenceDate) },
      gender = person.sexCode?.name,
      contacts = person.contacts.mapNotNull { contact -> contact.contactValue?.let { ApiResponseSetupContact(contact.contactType, it) } },
      ethnicity = person.ethnicityCode?.name,
      sexualOrientation = person.sexualOrientation?.name,

    )
  }
}
