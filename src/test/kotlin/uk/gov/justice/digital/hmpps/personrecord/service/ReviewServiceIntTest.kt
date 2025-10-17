package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.review.ClusterType
import uk.gov.justice.digital.hmpps.personrecord.service.review.ReviewService

class ReviewServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var reviewService: ReviewService

  @Autowired
  lateinit var reviewRepository: ReviewRepository

  @Test
  fun `should raise cluster for review`() {
    val primaryPersonKey = createPersonKey()
    val additionalPersonKey = createPersonKey()

    val review = reviewService.raiseForReview(primaryPersonKey, listOf(additionalPersonKey))

    assertThat(review.createdAt).isNotNull()
    assertThat(review.clusters).hasSize(2)

    val primary = review.clusters.find { it.clusterType == ClusterType.PRIMARY }
    assertThat(primary?.clusterType).isEqualTo(ClusterType.PRIMARY)
    assertThat(primary?.personKey?.personUUID).isEqualTo(primaryPersonKey.personUUID)

    val additional = review.clusters.find { it.clusterType == ClusterType.ADDITIONAL }
    assertThat(additional?.clusterType).isEqualTo(ClusterType.ADDITIONAL)
    assertThat(additional?.personKey?.personUUID).isEqualTo(additionalPersonKey.personUUID)
  }

  @Test
  fun `should not raise same primary cluster for review`() {
    val primaryPersonKey = createPersonKey()
    val additionalPersonKey = createPersonKey()

    val firstReview = reviewService.raiseForReview(primaryPersonKey, listOf(additionalPersonKey))
    val secondReview = reviewService.raiseForReview(primaryPersonKey, listOf(additionalPersonKey))

    assertThat(firstReview.id).isEqualTo(secondReview.id)
  }
}
