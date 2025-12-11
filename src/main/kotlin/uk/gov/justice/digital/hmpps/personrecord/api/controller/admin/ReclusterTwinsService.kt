package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import java.util.UUID

@Component
class ReclusterTwinsService(private val personKeyRepository: PersonKeyRepository, private val personRepository: PersonRepository,  private val publisher: ApplicationEventPublisher,
) {
  @Transactional
  fun split(clusterToSplit: List<String>) {
    val personKey = PersonKeyEntity.new()
    clusterToSplit.forEach {
      val personEntity = personRepository.findByMatchId(UUID.fromString(it))!!
      publisher.publishEvent(PersonKeyCreated(personEntity, personKey))
      personEntity.assignToPersonKey(personKey)
    }
    personKeyRepository.save(personKey)

  }

}
