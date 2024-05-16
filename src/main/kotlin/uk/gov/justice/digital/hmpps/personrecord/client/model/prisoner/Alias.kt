package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Alias(
  val firstName: String? = null,
  val lastName: String? = null,
  val middleNames: String?,
  val dob: LocalDate? = null,
)
