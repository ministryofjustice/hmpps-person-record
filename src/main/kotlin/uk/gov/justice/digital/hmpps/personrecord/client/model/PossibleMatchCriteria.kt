package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class PossibleMatchCriteria(
  val firstName: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val pncNumber: String? = null,
  val nomsNumber: String? = null
)
