package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jobs.BatchJob

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class ServiceNowMergeRequestJob(
  private val serviceNowMergeRequestService: ServiceNowMergeRequestService,
) : BatchJob {
  override val jobName = "GENERATE_DELIUS_MERGE_REQUESTS"

  override fun run() {
    serviceNowMergeRequestService.process()
  }
}
