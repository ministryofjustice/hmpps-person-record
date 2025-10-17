package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.review

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.review.ClusterType

@Entity
@Table(name = "review_cluster_link")
class ReviewClusterLinkEntity(

  @Id
  @ManyToOne
  @JoinColumn(name = "fk_review_id", nullable = false)
  val review: ReviewEntity,

  @Id
  @ManyToOne
  @JoinColumn(name = "fk_person_key_id")
  val personKey: PersonKeyEntity,

  @Enumerated(EnumType.STRING)
  val clusterType: ClusterType,

)
