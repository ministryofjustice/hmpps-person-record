package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_SEEDED

@RestController
@Profile("seeding")
class PopulateFromProbation(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  @Value("\${populate-from-probation.page-size}") val pageSize: Int,
  private val retryExecutor: RetryExecutor,
  private val personRepository: PersonRepository,
  private val eventLogRepository: EventLogRepository,
) {

  @Hidden
  @RequestMapping(method = [POST], value = ["/populatefromprobation"])
  suspend fun populate(): String {
    populatePages()
    return "OK"
  }

  suspend fun populatePages() {
    CoroutineScope(Dispatchers.Default).launch {
      val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
        CorePersonRecordAndDeliusClientPageParams(
          0,
          pageSize,
        ),
      )?.page?.totalPages?.toInt() ?: 1

      log.info("Starting DELIUS seeding, total pages: $totalPages")
      for (page in 0..<totalPages) {
        log.info("Processing DELIUS seeding, page: $page / $totalPages")
        retryExecutor.runWithRetryHTTP {
          corePersonRecordAndDeliusClient.getProbationCases(CorePersonRecordAndDeliusClientPageParams(page, pageSize))
        }?.cases?.map {
          val person = Person.from(it)
          PersonEntity.new(person)
        }?.let { personRepository.saveAll(it) }
          ?.map { EventLogEntity.from(it, CPR_RECORD_SEEDED) }
          ?.let { eventLogRepository.saveAll(it) }
      }
      log.info("DELIUS seeding finished, approx records ${totalPages * pageSize}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
