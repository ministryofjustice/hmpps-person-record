package uk.gov.justice.digital.hmpps.personrecord.model.commonplatform

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CPPersonDetails(
  val title: String? = null,
  val firstName: String? = null,
  val middleName: String? = null,
  val lastName: @NotBlank String,
  val dateOfBirth: LocalDate? = null,
  val gender: @NotBlank String,
  val address: @Valid CPAddress? = null,
)
