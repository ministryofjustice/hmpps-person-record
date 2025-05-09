package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_CREATED

private const val LOG_EVERY_X_RECORDS = 5

@RestController
@Profile("seeding")
class PopulateEventLog(
  private val eventLogRepository: EventLogRepository,
  private val personRepository: PersonRepository,
) {

  @Hidden
  @RequestMapping(method = [POST], value = ["/populateeventlog"])
  suspend fun populate(): String {
    populateEventLogFromPersonTable()
    return "OK"
  }

  suspend fun populateEventLogFromPersonTable() {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("Number of records to process is ${personRepository.count()}")

      personRepository.findAll().forEachIndexed { idx, person ->
        if (idx % LOG_EVERY_X_RECORDS == 0) {
          log.info("Populated $idx records in event log table")
        }
        eventLogRepository.save(EventLogEntity.from(personEntity = person, eventType = CPR_RECORD_CREATED, personKeyEntity = person.personKey))
      }
      log.info("Event log population finished")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
