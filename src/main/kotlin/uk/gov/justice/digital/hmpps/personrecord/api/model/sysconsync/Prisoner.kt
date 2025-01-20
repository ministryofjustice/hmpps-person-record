package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import java.time.LocalDate

data class Prisoner(
  val nationality: String? = null,
  val secondaryNationality: String? = null,
  val religion: List<Religion> = emptyList(),
  val prisonNumber: String? = null,
  val sexualOrientation: String? = null,
  val interestToImmigration: Boolean? = null,
  val disability: Boolean? = null,
  val status: String? = null,
  val sentenceStartDates: List<LocalDate> = emptyList(),
  val phoneNumbers: List<PhoneNumber> = emptyList(),
  val emails: List<Email> = emptyList(),
  val offenders: List<Names> = emptyList(),
  val addresses: List<Address> = emptyList(),
)

data class Religion(
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val religion: String? = null,
  val status: String? = null,
  val createdUserId: String? = null,
  val updatedUserId: String? = null,
)

data class PhoneNumber(
  val phoneId: Long? = null,
  val phoneNumber: String? = null,
  val phoneType: PhoneType? = null,
  val phoneExtension: String? = null,
)

data class Names(
  val title: String? = null,
  val firstName: String? = null,
  val middleName1: String? = null,
  val middleName2: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val birthPlace: String? = null,
  val birthCountry: String? = null,
  val nameType: NameType? = null,
  val sex: Sex? = null,
  val raceCode: String? = null,
  val created: LocalDate? = null,
  val offenderId: String? = null,
  val workingName: Boolean? = null,
  val identifiers: List<Identifier> = emptyList(),
)

data class Email(
  val id: Long? = null,
  val emailAddress: String? = null,
)

data class Address(
  val id: Long? = null,
  val type: String? = null,
  val flat: String? = null,
  val premise: String? = null,
  val street: String? = null,
  val locality: String? = null,
  val town: String? = null,
  val postcode: String? = null,
  val county: String? = null,
  val country: String? = null,
  val noFixedAddress: String? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val comment: String? = null,
  val isPrimary: Boolean? = null,
  val mail: Boolean? = null,
)

data class Identifier(
  val type: String? = null,
  val value: String? = null,
)

data class CreateResponse(
  val addressIds: List<AddressId> = emptyList(),
  val phoneIds: List<PhoneId> = emptyList(),
  val emailIds: List<EmailId> = emptyList(),
)

data class AddressId(
  val nomisAddressId: Long? = null,
  val cprAddressId: String? = null,
)

data class PhoneId(
  val nomisPhoneId: Long? = null,
  val cprPhoneId: String? = null,
)

data class EmailId(
  val nomisEmailId: Long? = null,
  val cprEmailId: String? = null,
)

enum class NameType {
  CURRENT,
  ALIAS,
  NICKNAME,
  MAIDEN,
}

enum class PhoneType {
  HOME,
  MOBILE,
  BUSINESS,
}

enum class Sex {
  MALE,
  FEMALE,
  NOT_KNOWN,
  NOT_SPECIFIED,
  REFUSED,
}
