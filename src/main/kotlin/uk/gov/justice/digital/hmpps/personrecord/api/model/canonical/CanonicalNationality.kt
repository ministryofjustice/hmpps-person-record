package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class CanonicalNationality(
  @Schema(description = "Nationality code", example = "UK")
  val nationalityCode: String? = "",

) {
  companion object {

    fun from(personEntity: PersonEntity): List<CanonicalNationality>? = personEntity.nationality?.let { listOf(CanonicalNationality(it)) }
  }
}
