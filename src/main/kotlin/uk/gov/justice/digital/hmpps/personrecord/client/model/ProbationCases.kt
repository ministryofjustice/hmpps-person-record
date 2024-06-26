package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.web.PagedModel.PageMetadata
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationCases(
  val page: PageMetadata,
  @JsonProperty("content")
  val cases: List<ProbationCase>,
)
