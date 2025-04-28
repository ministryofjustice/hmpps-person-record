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
  fun processDelete(event: String?, personCallback: () -> PersonEntity?) = fetchRecordAndDelete(event, personCallback)

  private fun fetchRecordAndDelete(event: String?, personCallback: () -> PersonEntity?) = personCallback()?.let { handleDeletion(event, it) }

  private fun handleDeletion(event: String?, personEntity: PersonEntity) {
    handlePersonKeyDeletion(personEntity)
    deletePersonRecord(personEntity)
    personMatchService.deleteFromPersonMatch(personEntity)
    handleMergedRecords(event, personEntity)
  }

  private fun handleMergedRecords(event: String?, personEntity: PersonEntity) {
    personEntity.id?.let {
      val mergedRecords: List<PersonEntity?> = personRepository.findByMergedTo(it)
      mergedRecords.forEach { mergedRecord ->
        fetchRecordAndDelete(event) { mergedRecord }
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
        else -> removeLinkToRecord(personEntity)
      }
    }
  }

  private fun deletePersonKey(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    personKeyRepository.delete(personKeyEntity)
    publisher.publishEvent(PersonKeyDeleted(personEntity, personKeyEntity))
  }

  private fun removeLinkToRecord(personEntity: PersonEntity) {
    personEntity.personKey?.personEntities?.remove(personEntity)
    personKeyRepository.save(personEntity.personKey!!)
  }
}
