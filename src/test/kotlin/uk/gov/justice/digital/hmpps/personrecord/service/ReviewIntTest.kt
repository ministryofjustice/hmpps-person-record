package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.review.ReviewClusterLinkEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.review.ReviewEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.review.ClusterType

class ReviewIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var reviewRepository: ReviewRepository

  @Test
  fun `should save review properly`() {
    val primaryPersonKey = createPersonKey()
    val additionalPersonKey = createPersonKey()

    val review = ReviewEntity.new()
    val clusterLinks = listOf(
      ReviewClusterLinkEntity(review = review, personKey = primaryPersonKey, clusterType = ClusterType.PRIMARY),
      ReviewClusterLinkEntity(review = review, personKey = additionalPersonKey, clusterType = ClusterType.ADDITIONAL),
    )

    review.clusters.addAll(clusterLinks)
    val savedReview = reviewRepository.save(review)

    assertThat(savedReview.createdAt).isNotNull()
    assertThat(savedReview.clusters).hasSize(2)

    val primary = savedReview.clusters.find { it.clusterType == ClusterType.PRIMARY }
    assertThat(primary?.clusterType).isEqualTo(ClusterType.PRIMARY)
    assertThat(primary?.personKey?.personUUID).isEqualTo(primaryPersonKey.personUUID)

    val additional = savedReview.clusters.find { it.clusterType == ClusterType.ADDITIONAL }
    assertThat(additional?.clusterType).isEqualTo(ClusterType.ADDITIONAL)
    assertThat(additional?.personKey?.personUUID).isEqualTo(additionalPersonKey.personUUID)
  }
}
