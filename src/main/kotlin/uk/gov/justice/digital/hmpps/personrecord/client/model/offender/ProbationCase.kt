package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationCase(
  val title: Value? = null,
  val name: ProbationCaseName,
  val ethnicity: Value? = null,
  val identifiers: Identifiers,
  val dateOfBirth: LocalDate? = null,
  val dateOfDeath: LocalDate? = null,
  val aliases: List<ProbationCaseAlias>? = emptyList(),
  val contactDetails: ContactDetails? = null,
  val addresses: List<ProbationAddress> = emptyList(),
  val sentences: List<Sentences>? = emptyList(),
  val nationality: Value? = null,
  val secondaryNationality: Value? = null,
  val gender: Value? = null,
  val sexualOrientation: Value? = null,
  val religion: Value? = null,
)
