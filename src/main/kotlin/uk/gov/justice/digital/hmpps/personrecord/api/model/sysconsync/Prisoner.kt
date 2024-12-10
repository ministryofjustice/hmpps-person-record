package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import java.time.LocalDate

data class Prisoner(
  val title: String? = null,
  val firstName: String? = null,
  val middleName1: String? = null,
  val middleName2: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val birthplace: String? = null,
  val nationality: String? = null,
  val multipleNationalities: List<String> = emptyList(),
  val religions: List<Religion> = emptyList(),
  val crn: String? = null,
  var prisonNumber: String? = null,
  val ethnicity: String? = null,
  val race: String? = null,
  val sexualOrientation: String? = null,
  val sex: String? = null,
  val interestToImmigration: Boolean? = null,
  val disability: Boolean? = null,
  val currentlyManaged: Boolean? = null,
  val sentenceStartDate: LocalDate? = null,
  val phoneNumbers: List<PhoneNumber> = emptyList(),
  val emails: List<String> = emptyList(),
  val aliases: List<Alias> = emptyList(),
  val addresses: List<Address> = emptyList(),
  val identifiers: List<Identifier> = emptyList(),
)

data class Religion(
  val effectiveDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val status: String? = null,
  val userId: String? = null,
)

data class PhoneNumber(
  val phoneId: String? = null,
  val phoneNumber: String? = null,
  val phoneType: String? = null,
  val phoneExtension: String? = null,
)

data class Alias(
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
  val isPrimary: Boolean,
  val mail: String? = null,
)

data class Identifier(
  val type: String? = null,
  val value: String? = null,
  val offenderId: String? = null,
)
