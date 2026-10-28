package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode

enum class ReferenceDataType(val key: String, val values: List<ReferenceData>) {
  SEX("sex", SexCode.entries),
  RELIGION("religion", ReligionCode.entries),
  NATIONALITY("nationality", NationalityCode.entries),
  COUNTRY("country", CountryCode.entries),
  TITLE("title", TitleCode.entries),
  ETHNICITY("ethnicity", EthnicityCode.entries),
  GENDER_IDENTITY("genderIdentity", GenderIdentityCode.entries),
  SEXUAL_ORIENTATION("sexualOrientation", SexualOrientation.entries),
  ADDRESS_STATUS("addressStatus", AddressStatusCode.entries),
  ADDRESS_USAGE("addressUsage", AddressUsageCode.entries),
  CONTACT_TYPE("contactType", ContactType.entries),
  ;

  companion object {
    fun fromKey(key: String): ReferenceDataType? = entries.find { it.key.equals(key, ignoreCase = true) }
  }
}
