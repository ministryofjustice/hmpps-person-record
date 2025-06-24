package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class DeletionService(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun processDelete(personCallback: () -> PersonEntity?) = fetchRecordAndDelete(personCallback)

  private fun fetchRecordAndDelete(personCallback: () -> PersonEntity?) {
    val person = personCallback() ?: throw PersonToDeleteNotFoundException("Could not find person to delete")
    handleDeletion(person)
  }

  private fun handleDeletion(personEntity: PersonEntity) {
    handlePersonKeyDeletion(personEntity)
    deletePersonRecord(personEntity)
    personMatchService.deleteFromPersonMatch(personEntity)
    handleMergedRecords(personEntity)
  }

  private fun handleMergedRecords(personEntity: PersonEntity) {
    personEntity.id?.let {
      val mergedRecords: List<PersonEntity?> = personRepository.findByMergedTo(it)
      mergedRecords.forEach { mergedRecord ->
        fetchRecordAndDelete { mergedRecord }
      }
    }
  }

  private fun deletePersonRecord(personEntity: PersonEntity) {
    personRepository.delete(personEntity)
    publisher.publishEvent(PersonDeleted(personEntity))
  }

  private fun handlePersonKeyDeletion(personEntity: PersonEntity) {
    personEntity.personKey?.let {
      when {
        it.personEntities.size == 1 -> deletePersonKey(it, personEntity)
        else -> removeLinkToRecord(it, personEntity)
      }
    }
  }

  private fun deletePersonKey(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    personKeyRepository.delete(personKeyEntity)
    publisher.publishEvent(PersonKeyDeleted(personEntity, personKeyEntity))
  }

  private fun removeLinkToRecord(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    personKeyEntity.personEntities.remove(personEntity)
    personKeyRepository.save(personKeyEntity)
  }
}

class PersonToDeleteNotFoundException(message: String) : RuntimeException(message)
