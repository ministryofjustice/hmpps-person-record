package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound

@Component
class PersonKeyService(
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
) {

  fun createPersonKey(personEntity: PersonEntity): PersonEntity {
    val personKey = PersonKeyEntity.new()
    publisher.publishEvent(PersonKeyCreated(personEntity, personKey))
    personKeyRepository.save(personKey)
    personEntity.personKey = personKey
    return personEntity
  }

  fun retrievePersonKey(personEntity: PersonEntity, highConfidenceRecord: PersonEntity): PersonEntity {
    publisher.publishEvent(PersonKeyFound(personEntity, highConfidenceRecord.personKey!!))
    personEntity.personKey = highConfidenceRecord.personKey!!
    return personEntity
  }
}
