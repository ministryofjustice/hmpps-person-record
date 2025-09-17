package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.PersonFactory
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import java.util.UUID

@Component
class PersonService(
  private val personFactory: PersonFactory,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  fun handlePersonCreation(person: Person): PersonEntity {
    val personEntity = personFactory.create(person)
    personMatchService.saveToPersonMatch(personEntity)
    if (person.linkOnCreate) {
      linkRecordToPersonKey(personEntity)
    }
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  fun handlePersonUpdate(person: Person, personEntity: PersonEntity): PersonEntity {
    val oldMatchingDetails = PersonMatchRecord.from(personEntity)
    personFactory.update(person, personEntity)
    val matchingFieldsHaveChanged = oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        personEntity,
      ),
    )
    when {
      matchingFieldsHaveChanged -> personMatchService.saveToPersonMatch(personEntity)
    }
    publisher.publishEvent(PersonUpdated(personEntity, matchingFieldsHaveChanged))
    personEntity.personKey?.let {
      if (person.reclusterOnUpdate && matchingFieldsHaveChanged) {
        reclusterService.recluster(personEntity)
      }
    }
    return personEntity
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity) {
    val matches = personMatchService.findClustersToJoin(personEntity)
    when {
      matches.containsExcluded() -> personEntity.setAsOverrideConflict()
      else -> personEntity.selectClusterToAssignTo(matches)
    }
  }

  private fun PersonEntity.selectClusterToAssignTo(matches: List<PersonMatchResult>) {
    if (matches.isEmpty()) {
      personKeyService.assignPersonToNewPersonKey(this)
    } else {
      personKeyService.assignToPersonKeyOfHighestConfidencePerson(
        this,
        matches.first().personEntity.personKey!!,
      )
      if (matches.size > 1) {
        reclusterService.recluster(this)
      }
    }
  }

  private fun PersonEntity.setAsOverrideConflict() {
    personKeyService.assignPersonToNewPersonKey(this)
    this.personKey?.setAsNeedsAttention(OVERRIDE_CONFLICT)
    publisher.publishEvent(
      RecordEventLog.from(
        CPRLogEvents.CPR_RECORD_CREATED_NEEDS_ATTENTION,
        this,
      ),
    )
  }

  private fun List<PersonMatchResult>.containsExcluded(): Boolean {
//  val scopes: Set<OverrideScopeEntity> = this.map { it.personEntity.overrideScopes }.flatten().toSet()
//  val scopedOverrideMarker: List<List<UUID>> = scopes.map { scope -> scope.personEntities.mapNotNull { it.overrideMarker } }
    val distinctCluster = this.collectDistinctClusters()
    val explodedScopesPerCluster = distinctCluster.map { cluster ->
      cluster.personEntities.map { person ->
        person.overrideMarker?.let { ExplodedScope(it, person.overrideScopes.map { scopeEntity -> scopeEntity.scope }) }
      }
    }
    return false
  }

  private data class ExplodedScope(
    val overrideMarker: UUID,
    val overrideScopes: List<UUID>
  )

  private fun List<PersonMatchResult>.collectDistinctClusters(): List<PersonKeyEntity> = this.map { it.personEntity }.groupBy { it.personKey!! }.map { it.key }.distinctBy { it.id }
}
