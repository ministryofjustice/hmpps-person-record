package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonClusterProcessor(
  private val publisher: ApplicationEventPublisher,
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
) {


  fun linkRecordToPersonKey(context: PersonContext): PersonClusterProcessor {
    val personEntityWithKey = personMatchService.findHighestConfidencePersonRecord(context.personEntity!!).exists(
      no = { createPersonKey(context.personEntity!!) },
      yes = { retrievePersonKey(context.personEntity!!, it) },
    )
    context.personEntity = personEntityWithKey
    return this
  }

  private fun createPersonKey(personEntity: PersonEntity): PersonEntity {
    val personKey = PersonKeyEntity.new()
    publisher.publishEvent(PersonKeyCreated(personEntity, personKey))
    personKeyRepository.save(personKey)
    personEntity.personKey = personKey
    return personEntity
  }

  private fun retrievePersonKey(personEntity: PersonEntity, highConfidenceRecord: PersonEntity): PersonEntity {
    publisher.publishEvent(PersonKeyFound(personEntity, highConfidenceRecord.personKey!!))
    personEntity.personKey = highConfidenceRecord.personKey!!
    return personEntity
  }
}