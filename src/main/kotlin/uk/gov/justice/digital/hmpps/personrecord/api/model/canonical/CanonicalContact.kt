package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity

data class CanonicalContact(
  @Schema(description = "Contact type")
  val type: CanonicalContactType,
  @Schema(description = "Contact value", example = "+44 20 7946 0000")
  val value: String? = null,
  @Schema(description = "Contact extension", example = "1234")
  val extension: String? = null,
) {

  companion object {
    fun from(contactEntity: ContactEntity): CanonicalContact = CanonicalContact(
      type = CanonicalContactType.from(contactEntity.contactType),
      value = contactEntity.contactValue,
      extension = contactEntity.extension,
    )

    fun fromContactEntityList(contactEntities: List<ContactEntity>): List<CanonicalContact> = contactEntities.map { from(it) }
  }
}
