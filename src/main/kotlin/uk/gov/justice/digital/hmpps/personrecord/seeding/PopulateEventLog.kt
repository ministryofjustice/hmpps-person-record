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
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents

@RestController
@Profile("seeding")
class PopulateEventLog(
  private val eventLogRepository: EventLogRepository,
  private val personRepository: PersonRepository,
) {

  @Hidden
  @RequestMapping(method = [POST], value = ["/populateeventlog"])
  suspend fun populate(): String {
    populateEventLog()
    return "OK"
  }

  suspend fun populateEventLog() {
    CoroutineScope(Dispatchers.Default).launch {
      personRepository.findAll().forEach { person ->
        eventLogRepository.saveAndFlush(EventLogEntity.from(personEntity = person, eventType = CPRLogEvents.CPR_RECORD_CREATED, personKeyEntity = person.personKey))
      }
      log.info("Event log population finished")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
