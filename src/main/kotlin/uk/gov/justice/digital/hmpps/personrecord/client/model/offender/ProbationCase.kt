package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationCase(
  val title: Title? = null,
  val name: Name,
  val identifiers: Identifiers,
  val dateOfBirth: LocalDate? = null,
  val aliases: List<ProbationCaseAlias>? = emptyList(),
  val contactDetails: ContactDetails? = null,
  val addresses: List<Address> = emptyList(),
  val sentences: List<Sentences>? = emptyList(),
)
