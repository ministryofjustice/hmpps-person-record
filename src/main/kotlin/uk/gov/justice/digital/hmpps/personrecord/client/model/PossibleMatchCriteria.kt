package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class PossibleMatchCriteria(
  val firstName: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
  val pncNumber: String?,
  val nomsNumber: String?
)
