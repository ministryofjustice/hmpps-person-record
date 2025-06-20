package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonKeyService(
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
  private val personMatchService: PersonMatchService,
) {

  fun assignPersonToNewPersonKey(personEntity: PersonEntity): PersonEntity {
    val personKey = PersonKeyEntity.new()
    publisher.publishEvent(PersonKeyCreated(personEntity, personKey))
    personKeyRepository.save(personKey)
    personEntity.personKey = personKey
    return personEntity
  }

  fun assignToPersonKeyOfHighestConfidencePerson(personEntity: PersonEntity, highConfidenceRecord: PersonEntity): PersonEntity {
    publisher.publishEvent(PersonKeyFound(personEntity, highConfidenceRecord.personKey!!))
    personEntity.personKey = highConfidenceRecord.personKey!!
    return personEntity
  }

  fun clusterIsNeedsAttentionAndCanBecomeActive(cluster: PersonKeyEntity): Boolean = cluster.isNeedsAttention() && personMatchService.examineIsClusterValid(cluster).isClusterValid

  fun settingNeedsAttentionClusterToActive(personKeyEntity: PersonKeyEntity, changedRecord: PersonEntity) {
    personKeyEntity.status = ACTIVE
    personKeyRepository.save(personKeyEntity)
    publisher.publishEvent(
      RecordEventLog(
        CPRLogEvents.CPR_NEEDS_ATTENTION_TO_ACTIVE,
        changedRecord,
        personKeyEntity,
      ),
    )
  }

}
