package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate


@JsonIgnoreProperties(ignoreUnknown = true)
data class Prisoner(
  val prisonerNumber: String,
  val pncNumber: String?,
  val croNumber: String?,
  val firstName: String,
  val middleNames: String?,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val gender: String,
  val nationality: String
)
