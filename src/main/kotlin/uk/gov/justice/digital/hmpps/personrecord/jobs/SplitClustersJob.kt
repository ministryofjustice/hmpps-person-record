package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.springframework.stereotype.Component

@Component
class SplitClustersJob : BatchJob {
  override val jobName = "SPLIT_CLUSTERS"
  override fun run() { }
}
