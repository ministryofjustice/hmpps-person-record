package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffenderDetail(
  val offenderId: Long,
  val title: String? = null,
  val firstName: String,
  val middleNames: List<String>? = emptyList(),
  val surname: String,
  val dateOfBirth: LocalDate,
  val otherIds: IDs,
  val previousSurName: String? = null,
  val gender: String? = null,
  val offenderAliases: List<Alias> = emptyList(),
  val contactDetails: ContactDetails? = null,
  val offenderProfile: OffenderProfile? = null,
  val highlight: Highlight? = null,
)
