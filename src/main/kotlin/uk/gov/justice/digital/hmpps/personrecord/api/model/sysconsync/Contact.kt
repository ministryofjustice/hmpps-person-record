package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

data class ContactInfo(
  val phoneNumbers: List<Phone>,
  val emails: List<Email>,
)

data class Phone(
  val id: String?,
  val value: String?,
  val type: PhoneType,
  val extension: String?,
)

data class Email(
  val id: String?,
  val value: String?,
)

enum class PhoneType {
  BUS,
  FAX,
  HOME,
  MOB,
  ALTH,
}
