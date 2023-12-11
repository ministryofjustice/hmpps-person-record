package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffenderDetail(
  val offenderId: Long,
  val firstName: String,
  val surname: String,
  val dateOfBirth: LocalDate,
  val otherIds: IDs,
)
