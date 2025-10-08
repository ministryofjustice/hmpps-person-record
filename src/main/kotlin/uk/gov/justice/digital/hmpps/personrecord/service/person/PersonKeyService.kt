package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonKeyService(
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  fun linkRecordToPersonKey(personEntity: PersonEntity) {
    val matches = personMatchService.findClustersToJoin(personEntity)
    when {
      matches.containsExcluded() -> personEntity.setAsOverrideConflict()
      else -> personEntity.selectClusterToAssignTo(matches)
    }
  }

  private fun assignPersonToNewPersonKey(personEntity: PersonEntity) {
    val personKey = PersonKeyEntity.new()
    publisher.publishEvent(PersonKeyCreated(personEntity, personKey))
    personEntity.assignToPersonKey(personKey)
    personKeyRepository.save(personKey)
  }

  private fun assignToPersonKeyOfHighestConfidencePerson(personEntity: PersonEntity, personKey: PersonKeyEntity) {
    publisher.publishEvent(PersonKeyFound(personEntity, personKey))
    personEntity.assignToPersonKey(personKey)
  }

  private fun PersonEntity.selectClusterToAssignTo(matches: List<PersonMatchResult>) {
    if (matches.isEmpty()) {
      assignPersonToNewPersonKey(this)
    } else {
      assignToPersonKeyOfHighestConfidencePerson(
        this,
        matches.first().personEntity.personKey!!,
      )
      if (matches.size > 1) {
        reclusterService.recluster(this)
      }
    }
  }

  private fun PersonEntity.setAsOverrideConflict() {
    assignPersonToNewPersonKey(this)
    this.personKey?.setAsNeedsAttention(OVERRIDE_CONFLICT)
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
