package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.review

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.review.ClusterType
import java.time.LocalDateTime

@Entity
@Table(name = "review")
class ReviewEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "created_at")
  val createdAt: LocalDateTime? = null,

  @Column(name = "resolved_at")
  val resolvedAt: LocalDateTime? = null,

  @OneToMany(mappedBy = "review", cascade = [CascadeType.ALL])
  val clusters: MutableList<ReviewClusterLinkEntity> = mutableListOf(),

) {

  fun addPrimary(personKey: PersonKeyEntity) = this.clusters.add(
    ReviewClusterLinkEntity(
      review = this,
      personKey = personKey,
      clusterType = ClusterType.PRIMARY,
    ),
  )

  fun addAdditional(personKeyEntities: List<PersonKeyEntity>) = personKeyEntities.forEach { cluster ->
    this.clusters.add(
      ReviewClusterLinkEntity(
        review = this,
        personKey = cluster,
        clusterType = ClusterType.ADDITIONAL,
      ),
    )
  }

  companion object {
    fun new(): ReviewEntity = ReviewEntity(
      createdAt = LocalDateTime.now(),
    )
  }
}
