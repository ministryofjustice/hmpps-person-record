package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class ContactInfo(
  val phoneNumbers: List<Phone>,
  val emails: List<Email>,
)

data class Phone(
  @Schema(description = "The phone ID", example = "6be22c55-7572-4031-b5b0-2546bea6f6a1")
  val id: String?,
  @Schema(description = "The phone value", example = "01234567890")
  val value: String?,
  @Schema(description = "The phone type", example = "MOB")
  val type: PhoneType,
  @Schema(description = "The phone extension", example = "+44")
  val extension: String?,
)

data class Email(
  @Schema(description = "The email ID", example = "9be2496d-b253-4b1f-ab65-786d58acc629")
  val id: String?,
  @Schema(description = "The email value", example = "email@email.com")
  val value: String?,
)

enum class PhoneType {
  BUS,
  FAX,
  HOME,
  MOB,
  ALTH,
}
