package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import java.time.LocalDate

data class Prisoner(
  val birthPlace: String? = null,
  val birthCountry: String? = null,
  val nationality: String? = null,
  val secondaryNationality: String? = null,
  val religion: String? = null,
  val prisonNumber: String? = null,
  val sexualOrientation: String? = null,
  val interestToImmigration: Boolean? = null,
  val disability: Boolean? = null,
  val currentlyManaged: Boolean? = null,
  val sentenceStartDates: List<LocalDate> = emptyList(),
  val phoneNumbers: List<PhoneNumber> = emptyList(),
  val emails: List<String> = emptyList(),
  val offenders: List<Names> = emptyList(),
  val addresses: List<Address> = emptyList(),
)

// leaving this in right now, even though not referenced above as it likely to be needed in the very near future
data class Religion(
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val religion: String? = null,
  val status: String? = null,
  val userId: String? = null,
)

data class PhoneNumber(
  val phoneId: String? = null,
  val phoneNumber: String? = null,
  val phoneType: String? = null,
  val phoneExtension: String? = null,
)

data class Names(
  val title: String? = null,
  val firstName: String? = null,
  val middleName1: String? = null,
  val middleName2: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val type: String? = null,
  val sex: String? = null,
  val ethnicity: String? = null,
  val race: String? = null,
  val created: LocalDate? = null,
  val offenderId: String? = null,
  val workingName: Boolean? = null,
  val identifiers: List<Identifier> = emptyList(),
)

data class Address(
  val id: String? = null,
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
)

data class AddressId(
  val nomisAddressId: String? = null,
  val cprAddressId: String? = null,
)

data class PhoneId(
  val nomisPhoneId: String? = null,
  val cprPhoneId: String? = null,
)
