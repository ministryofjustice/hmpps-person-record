package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class PhysicalAttributes(
  val sexCode: String? = null,
  val raceCode: String? = null,
  val ethnicity: String? = null,
)
