package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase

@Entity
@Table(name = "court_probation_link_table")
class CourtProbationLinkEntity(

  @Id
  @Column(name = "defendant_id")
  val defendantId: String,

  @Column
  val crn: String,
) {
  companion object {

    fun from(probationCase: ProbationCase): CourtProbationLinkEntity = CourtProbationLinkEntity(
      defendantId = probationCase.identifiers.defendantId!!,
      crn = probationCase.identifiers.crn!!,
    )
  }
}
