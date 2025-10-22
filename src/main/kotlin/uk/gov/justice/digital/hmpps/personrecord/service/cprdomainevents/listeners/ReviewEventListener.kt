package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.review.ReviewRaised
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.review.ReviewRemoved
import uk.gov.justice.digital.hmpps.personrecord.service.review.ReviewService

@Component
class ReviewEventListener(
  private val reviewService: ReviewService,
) {

  @Async
  @EventListener
  @TransactionalEventListener
  fun onReviewRaised(reviewRaised: ReviewRaised) {
    reviewService.create(reviewRaised.primaryCluster, reviewRaised.additionalClusters)
  }

  @Async
  @EventListener
  @TransactionalEventListener
  fun onReviewRemoved(reviewRemoved: ReviewRemoved) {
    reviewService.delete(reviewRemoved.primaryCluster)
  }
}
