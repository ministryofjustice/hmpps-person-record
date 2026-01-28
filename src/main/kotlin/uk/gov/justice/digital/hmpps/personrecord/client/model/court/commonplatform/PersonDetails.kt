package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonDetails(
  val title: String? = null,
  val firstName: String? = null,
  val middleName: String? = null,
  val lastName: @NotBlank String,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val address: @Valid Address? = null,
  val contact: Contact? = null,
  val ethnicity: Ethnicity? = null,
  val nationalityCode: String? = null,
  val additionalNationalityCode: String? = null,
  val nationalInsuranceNumber: String? = null,
)
