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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClientPageParams
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@RestController
@Profile("seeding")
class UpdateFromProbation(
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  @Value("\${populate-from-probation.page-size}") private val pageSize: Int,
  private val repository: PersonRepository,
  private val retryExecutor: RetryExecutor,
) {

  @Hidden
  @RequestMapping(method = [POST], value = ["/updatefromprobation"])
  suspend fun populate(@RequestParam startPage: Int = 0): String {
    populatePages(startPage)
    return "OK"
  }

  suspend fun populatePages(startPage: Int) {
    CoroutineScope(Dispatchers.Default).launch {
      val totalPages = corePersonRecordAndDeliusClient.getProbationCases(
        CorePersonRecordAndDeliusClientPageParams(
          startPage,
          pageSize,
        ),
      )?.page?.totalPages?.toInt() ?: 1

      log.info("Starting DELIUS updates, starting page $startPage, total pages: $totalPages")
      for (page in startPage..<totalPages) {
        log.info("Processing DELIUS updates, page: $page / $totalPages")
        retryExecutor.runWithRetryHTTP {
          corePersonRecordAndDeliusClient.getProbationCases(CorePersonRecordAndDeliusClientPageParams(page, pageSize))
        }?.cases?.forEach {
          val person = Person.from(it)

          repository.findByCrn(person.crn!!).exists(
            no = {
              val personToSave = PersonEntity.new(person)
              repository.saveAndFlush(personToSave)
            },
            yes = {
              it.update(person)
              repository.saveAndFlush(it)
            },
          )
        }
      }
      log.info("DELIUS updates finished, approx records ${totalPages * pageSize}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
