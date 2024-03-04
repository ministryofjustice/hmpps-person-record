package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrisonerDetails(
  val offenderNo: String? = null,
  val offenderId: Long? = null,
  val rootOffenderId: Long? = null,
  val title: String? = null,
  val firstName: String? = null,
  val middleName: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val nationality: String? = null,
)
