package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class CanonicalNationality(
  @Schema(description = "person nationality code", example = "GB")
  val code: String? = null,
  @Schema(description = "Person nationality description", example = "GB")
  val description: String? = null,

) {
  companion object {

    fun from(personEntity: PersonEntity): List<CanonicalNationality> = personEntity.nationality?.let {
      listOf(
        CanonicalNationality(
          code = personEntity.nationality,
          description = personEntity.nationality,
        ),
      )
    } ?: emptyList()
  }
}
