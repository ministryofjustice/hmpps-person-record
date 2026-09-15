package uk.gov.justice.digital.hmpps.personrecord.jobs.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase

@Component
class RetryableProbationUpdater(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val transactionalProbationUpdater: TransactionalProbationUpdater,
) {

  @MigrationWebRetryable
  fun repopulateProbationRecord(pageParams: CorePersonRecordAndDeliusClientPageParams) {
    corePersonRecordAndDeliusClient.getProbationCases(pageParams)?.cases.orEmpty()
      .forEach { probationCase ->
        val hasLowercasePnc = probationCase.hasLowercasePnc()
        val hasLowercaseCro = probationCase.hasLowercaseCro()

        if (hasLowercasePnc || hasLowercaseCro) {
          log.info("Updating probation record {} with lowercase identifiers: pnc={}, cro={}", probationCase.identifiers.crn, hasLowercasePnc, hasLowercaseCro)
          transactionalProbationUpdater.update(probationCase)
        }
      }
  }

  fun ProbationCase.hasLowercasePnc(): Boolean = identifiers.pnc?.any { it.isLowerCase() } == true

  fun ProbationCase.hasLowercaseCro(): Boolean = identifiers.cro?.any { it.isLowerCase() } == true

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
