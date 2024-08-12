package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Sentences(
  @JsonProperty("date")
  val sentenceDate: LocalDate? = null,
)
