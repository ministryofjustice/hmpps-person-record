package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyDeleted

@Component
class PersonKeyDeletionService(
  private val personKeyRepository: PersonKeyRepository,
  private val reviewRepository: ReviewRepository,
  private val publisher: ApplicationEventPublisher,
) {

  fun deletePersonKey(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    reviewRepository.findByClustersPersonKey(personKeyEntity)?.let { review ->
      reviewRepository.delete(review)
    }
    personKeyRepository.delete(personKeyEntity)
    publisher.publishEvent(PersonKeyDeleted(personEntity, personKeyEntity))
  }
}
