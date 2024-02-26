package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeliusOffenderDetail(
  val name: Name,
  val identifiers: Identifiers,
  val dateOfBirth: LocalDate? = null,

)
