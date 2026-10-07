package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContactType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import java.time.LocalDateTime

@Schema(description = "A prisoner contact")
data class PrisonContactResponse(
  @Schema(description = "The CPR contact id", example = "f4165b62-d9eb-406c-bfb1-4f54f4bd2c6d")
  val contactId: String,
  @Schema(description = "Contact type")
  val type: CanonicalContactType,
  @Schema(description = "Contact value", example = "+44 20 7946 0000")
  val value: String,
  @Schema(description = "Contact extension", example = "1234")
  val extension: String? = null,
  @Schema(description = "Date contact was created", example = "2023-01-01T12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that added contact")
  val createUserId: String? = null,
  @Schema(description = "Date contact was last modified", example = "2023-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that last modified contact")
  val modifyUserId: String? = null,
) {
  companion object {
    fun from(contactEntity: ContactEntity) = PrisonContactResponse(
      contactId = contactEntity.updateId.toString(),
      type = CanonicalContactType.from(contactEntity.contactType),
      value = contactEntity.contactValue.orEmpty(),
      extension = contactEntity.extension,
      createDateTime = contactEntity.createDateTime,
      createUserId = contactEntity.createUserId,
      modifyDateTime = contactEntity.modifyDateTime,
      modifyUserId = contactEntity.modifyUserId,
    )
  }
}
