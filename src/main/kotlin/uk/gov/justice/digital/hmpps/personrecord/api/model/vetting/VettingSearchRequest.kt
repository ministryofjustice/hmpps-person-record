package uk.gov.justice.digital.hmpps.personrecord.api.model.vetting

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class VettingSearchRequest(
  val fullName: String,
  val dateOfBirth: LocalDate,
  val firstNameAliases: List<String> = emptyList(),
  val lastNameAliases: List<String> = emptyList(),
  val dateOfBirthAliases: List<LocalDate> = emptyList(),
  val postcodes: List<String> = emptyList(),
)
