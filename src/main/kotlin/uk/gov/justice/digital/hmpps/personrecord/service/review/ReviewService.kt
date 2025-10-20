package uk.gov.justice.digital.hmpps.personrecord.service.review

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.review.ReviewEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.review.ClusterType

@Component
class ReviewService(
  private val reviewRepository: ReviewRepository,
) {

  fun create(primary: PersonKeyEntity, additional: List<PersonKeyEntity>? = null): ReviewEntity {
    val reviewEntity = primary.getIfReviewExists()
    return when {
      reviewEntity == null -> primary.createNewReview(additional)
      else -> reviewEntity
    }
  }

  fun delete(cluster: PersonKeyEntity) = cluster.getIfReviewExists()?.let { reviewRepository.delete(it) }

  private fun PersonKeyEntity.createNewReview(additional: List<PersonKeyEntity>? = null): ReviewEntity {
    val review = ReviewEntity.new()
    review.addPrimary(this)
    additional?.let { review.addAdditional(it) }
    return reviewRepository.save(review)
  }

  private fun PersonKeyEntity.getIfReviewExists() = reviewRepository.findByClustersClusterTypeAndClustersPersonKey(ClusterType.PRIMARY, this)
}
