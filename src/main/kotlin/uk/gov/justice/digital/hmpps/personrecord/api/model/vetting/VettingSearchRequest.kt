package uk.gov.justice.digital.hmpps.personrecord.api.model.vetting

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class VettingSearchRequest(
  val fullName: String,
  val dateOfBirth: LocalDate,
  val firstNameAliases: List<String>,
  val lastNameAliases: List<String>,
  val dateOfBirthAliases: List<LocalDate>,
  val postcodes: List<String>,
)
