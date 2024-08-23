package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class AllConvictedOffences(
  @JsonProperty("sentenceStartDate")
  val sentenceStartDate: LocalDate? = null,
  @JsonProperty("primarySentence")
  val primarySentence: Boolean? = null,
)
