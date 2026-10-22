package uk.gov.justice.digital.hmpps.personrecord.jobs.splitclusters

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.personrecord.client.WebRetryable
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.DatabaseRetryable
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class RetryableClusterSplitService(
  private val transactionalClusterSplitService: TransactionalClusterSplitService,
  private val personMatchService: PersonMatchService,
) {

  @DatabaseRetryable
  fun split(recordsToSplit: List<String>) = transactionalClusterSplitService.split(recordsToSplit)

  @WebRetryable(retryFor = [WebClientResponseException::class])
  fun examineIsClusterValid(cluster: PersonKeyEntity): IsClusterValidResponse =
    personMatchService.examineIsClusterValid(cluster)
}