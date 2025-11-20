package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

data class CanonicalNationality(
  @Schema(description = "person nationality code", example = "GB")
  val code: String? = null,
  @Schema(description = "Person nationality description", example = "GB")
  val description: String? = null,
  @Schema(description = "Person nationality start date", example = "01/01/2000")
  val startDate: LocalDate? = null,
  @Schema(description = "Person nationality end date", example = "12/08/2025")
  val endDate: LocalDate? = null,
  @Schema(description = "Person nationality notes", example = "Example notes")
  val notes: String? = null,

) {
  companion object {

    fun from(personEntity: PersonEntity): List<CanonicalNationality> = personEntity.nationalities.map {
      CanonicalNationality(
        code = it.nationalityCode.name,
        description = it.nationalityCode.description,
        startDate = it.startDate,
        endDate = it.startDate,
        notes = it.notes,
      )
    }
  }
}
