package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
@Profile("!preprod & !prod")
class SplitClustersJob : BatchJob {
  override val jobName = "SPLIT_CLUSTERS"

  override fun run() {
    log.info("Running cluster split job")
  }

  companion object {
    private val log = LoggerFactory.getLogger(SplitClustersJob::class.java)
  }
}
