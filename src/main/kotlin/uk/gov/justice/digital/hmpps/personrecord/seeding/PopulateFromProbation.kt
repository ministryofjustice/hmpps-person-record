package uk.gov.justice.digital.hmpps.personrecord.seeding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor

@RestController
class PopulateFromProbation(
  val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  @Value("\${populate-from-probation.page-size}") val pageSize: Int,
  @Value("\${populate-from-probation.retry.delay}") val delayMillis: Long,
  @Value("\${populate-from-probation.retry.times}") val retries: Int,
  val repository: PersonRepository,
) {

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
      )?.totalPages ?: 1

      log.info("Starting DELIUS seeding, total pages: $totalPages")
      for (page in 0..<totalPages) {
        RetryExecutor.runWithRetry(retries, delayMillis) {
          corePersonRecordAndDeliusClient.getProbationCases(CorePersonRecordAndDeliusClientPageParams(page, pageSize))
        }?.cases?.forEach {
          val person = Person.from(it)
          val personToSave = PersonEntity.from(person)
          repository.saveAndFlush(personToSave)
        }
      }
      log.info("DELIUS seeding finished, approx records ${totalPages * pageSize}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
