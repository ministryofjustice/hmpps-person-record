package uk.gov.justice.digital.hmpps.personrecord.jobs.splitclusters

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyLeave
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeySplit
import java.util.UUID

@Component
class TransactionalClusterSplitService(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun split(recordsToSplit: List<String>) {
    val personKey = PersonKeyEntity.new()
    recordsToSplit.forEachIndexed { index, matchId ->
      val personEntity = personRepository.findByMatchId(UUID.fromString(matchId))!!
      publisher.publishEvent(PersonKeyLeave(personEntity, personEntity.personKey!!))
      personEntity.assignToPersonKey(personKey)
      personKeyRepository.save(personKey)
      if (index == 0) {
        publisher.publishEvent(PersonKeyCreated(personEntity, personKey))
      }
      publisher.publishEvent(PersonKeySplit(personEntity, personKey))
    }
    log.info("Created cluster ${personKey.personUUID} with these matchIds: ${personKey.personEntities.map { it.matchId }}")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
