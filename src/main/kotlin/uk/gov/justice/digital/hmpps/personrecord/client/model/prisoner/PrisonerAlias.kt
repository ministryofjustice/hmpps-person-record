package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrisonerAlias(
  val title: String? = null,
  val firstName: String? = null,
  val lastName: String? = null,
  val middleNames: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
)
