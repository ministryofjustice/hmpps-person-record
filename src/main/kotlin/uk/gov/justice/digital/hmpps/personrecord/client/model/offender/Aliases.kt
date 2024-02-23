package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Aliases(
  val firstName: String? = null,
  val surName: String? = null,
  val middleNames: List<String>? = emptyList(),
  val dataOfBirth: LocalDate? =  null,
  val id: String? = null,
)
