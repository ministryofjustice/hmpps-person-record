package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class NewMergeService(
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val deletionService: DeletionService,
) {

  @Transactional
  fun processMerge(from: PersonEntity?, to: PersonEntity) {
    if (fromRecordIsAloneOnSeparateCluster(from, to)) {
      markClusterAsMerged(from, to)
    }
    merge(from, to)
  }

  private fun markClusterAsMerged(from: PersonEntity?, to: PersonEntity) {
    from?.personKey?.let {
      it.markAsMerged(to.personKey!!)
      personKeyRepository.save(it)
    }
  }

  private fun merge(from: PersonEntity?, to: PersonEntity) {
    from?.let {
      it.removePersonKeyLink()
      it.mergeTo(to)
      personRepository.save(it)
      deletionService.deletePersonFromPersonMatch(it)
    }
  }

  private fun fromRecordIsAloneOnSeparateCluster(from: PersonEntity?, to: PersonEntity): Boolean = fromClusterHasOneRecord(from) and isDifferentUuid(from, to)

  private fun fromClusterHasOneRecord(from: PersonEntity?): Boolean = (from?.personKey?.personEntities?.size ?: 0) == 1

  private fun isDifferentUuid(from: PersonEntity?, to: PersonEntity): Boolean = from?.personKey?.id != to.personKey?.id
}
