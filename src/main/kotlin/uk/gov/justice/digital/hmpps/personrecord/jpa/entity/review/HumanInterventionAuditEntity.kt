package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.review

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "human_intervention_audit")
class HumanInterventionAuditEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "human_intervention_id")
  val resolvedBy: String? = null,

  @Column(name = "decision_rationale")
  val decisionRationale: String? = null,

)
