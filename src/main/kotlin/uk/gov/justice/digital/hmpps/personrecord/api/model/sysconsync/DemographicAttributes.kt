package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

data class DemographicAttributes(
  val dateOfBirth: String?,
  val birthPlace: String?,
  val birthCountryCode: String?,
  val nationalityCode: String?,
  val multipleNationalities: String?,
  val ethnicityCode: String?,
  val sexualOrientationCode: String?,
  val sexCode: String?,
  val disability: String?,
  val interestToImmigration: String?,
)
