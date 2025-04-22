package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonKeyService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
) {

  fun getOrCreatePersonKey(personEntity: PersonEntity): PersonEntity {
    val highConfidenceRecord: PersonEntity? = personMatchService.findHighestConfidencePersonRecord(personEntity)
    val key = when {
      highConfidenceRecord == PersonEntity.empty -> createPersonKey(personEntity)
      else -> retrievePersonKey(personEntity, highConfidenceRecord)
    }
    personEntity.personKey = key
    return personEntity
  }

  private fun createPersonKey(personEntity: PersonEntity): PersonKeyEntity {
    val personKey = PersonKeyEntity.new()
    publisher.publishEvent(PersonKeyCreated(personEntity, personKey))
    return personKeyRepository.save(personKey)
  }

  private fun retrievePersonKey(personEntity: PersonEntity, highConfidenceRecord: PersonEntity): PersonKeyEntity {
    publisher.publishEvent(PersonKeyFound(personEntity, highConfidenceRecord.personKey!!))
    return highConfidenceRecord.personKey!!
  }
}
