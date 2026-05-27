package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents.CPR_RECORD_SEEDED
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class TransactionalProbationUpdater(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun update(case: ProbationCase) {
    val person = Person.from(case)
    personRepository.findByCrn(person.crn!!).exists(
      no = {
        log.error("CRN not found in Database ${person.crn}")
        personService.processPerson(person, { personRepository.findByCrn(person.crn) })
        publisher.publishEvent(RecordEventLog(CPR_RECORD_SEEDED, personRepository.findByCrn(person.crn)!!))
      },
      yes = {
        if (it.isNotMerged()) {
          it.update(person)
          personRepository.save(it)
        }
      },
    )
  }
  private fun PersonEntity?.exists(no: () -> Unit, yes: (personEntity: PersonEntity) -> Unit) = when {
    this == null -> no()
    else -> yes(this)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
