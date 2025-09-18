package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.PersonChainable
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.PersonFactory
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personFactory: PersonFactory,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  fun create(person: Person): PersonEntity {
    val ctx = personFactory.create(person)
      .linkToPersonKey()
      .saveToPersonMatch()
    publisher.publishEvent(PersonCreated(ctx.personEntity))
    return ctx.personEntity
  }

  fun update(person: Person, personEntity: PersonEntity): PersonEntity {
    val ctx = personFactory.update(person, personEntity)
      .saveToPersonMatch()
    publisher.publishEvent(PersonUpdated(ctx.personEntity, ctx.matchingFieldsChanged))
    personEntity.personKey?.let {
      if (person.reclusterOnUpdate && ctx.matchingFieldsChanged) {
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

  private fun PersonChainable.saveToPersonMatch(): PersonChainable {
    when {
      this.matchingFieldsChanged -> personMatchService.saveToPersonMatch(this.personEntity)
    }
    return this
  }

  private fun PersonChainable.linkToPersonKey(): PersonChainable {
    when {
      this.linkOnCreate -> linkRecordToPersonKey(this.personEntity)
    }
    return this
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
    val scopesPerCluster = this.collectDistinctClusters()
      .map { cluster ->
        cluster.personEntities
          .flatMap { person ->
            person.overrideScopes
              .map { scopeEntity -> scopeEntity.scope }
              .toSet()
          }
      }
    val allScopesAcrossClusters = scopesPerCluster.flatten()
    val hasSameScopes = allScopesAcrossClusters.size != allScopesAcrossClusters.toSet().size
    return hasSameScopes
  }

  private fun List<PersonMatchResult>.collectDistinctClusters(): List<PersonKeyEntity> = this
    .map { it.personEntity }
    .groupBy { it.personKey!! }
    .map { it.key }
    .distinctBy { it.id }
}
