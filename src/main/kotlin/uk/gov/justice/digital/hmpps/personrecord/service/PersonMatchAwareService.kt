package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

abstract class PersonMatchAwareService<I, O>(private val dependencies: PersonMatchAwareServiceDependencies) {

  protected abstract fun process(input: I): O

  fun handle(input: I): O {
    val output = process(input)

    // CREATED?
      // person key stuff
      // person match stuff
      // app event stuff (telemetry, logging, etc...)

    // UPDATED?
      // person key stuff
      // person match stuff
      // app event stuff (telemetry, logging, etc...)
      // recluster stuff

    return output
  }
}

class PersonMatchAwareServiceDependencies(
  val personRepository: PersonRepository,
  val personMatchService: PersonMatchService,
  val publisher: ApplicationEventPublisher,
  val personKeyService: PersonKeyService,
  val reclusterService: ReclusterService,
)