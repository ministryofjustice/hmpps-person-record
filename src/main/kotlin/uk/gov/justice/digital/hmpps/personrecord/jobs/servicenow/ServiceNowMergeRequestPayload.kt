package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.extensions.getPNCs
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

data class ProbationRecord(
  @JsonProperty("full_name_b") val fullName: String,
  @JsonProperty("date_of_birth_b") val dateOfBirth: LocalDate?,
  @JsonProperty("case_reference_number_crn_a") val crn: String,
  @JsonProperty("police_national_computer_pnc_reference_b") val pnc: String?,
) {
  companion object {
    fun from(personEntity: PersonEntity): ProbationRecord = ProbationRecord(fullName = "", dateOfBirth = personEntity.getPrimaryName().dateOfBirth, crn = personEntity.crn!!, pnc = personEntity.references.getPNCs().first())
  }
}

data class Variables(
  val requester: String,
  @JsonProperty("requested_for") val requestedFor: String,
  @JsonProperty("record_a_details_cpr_ndelius") val details: List<ProbationRecord>,
)

data class ServiceNowMergeRequestPayload(
  @JsonProperty("sysparm_id") val sysParmId: String,
  @JsonProperty("sysparm_quantity") val quantity: Int,
  val variables: Variables,
)
