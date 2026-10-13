package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.review.ReviewRaised
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import java.util.UUID

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
      matches.containsExcluded() -> personEntity.setAsOverrideConflict(matches)
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

  private fun PersonEntity.selectClusterToAssignTo(matches: List<PersonKeyEntity>) {
    if (matches.isEmpty()) {
      assignPersonToNewPersonKey(this)
    } else {
      assignToPersonKeyOfHighestConfidencePerson(
        this,
        matches.first(),
      )
      if (matches.size > 1) {
        reclusterService.recluster(this)
      }
    }
  }

  private fun PersonEntity.setAsOverrideConflict(matchingClusters: List<PersonKeyEntity>) {
    assignPersonToNewPersonKey(this)
    this.personKey?.let {
      it.setAsNeedsAttention(OVERRIDE_CONFLICT)
      publisher.publishEvent(ReviewRaised(it, matchingClusters))
    }
  }

  /**
   * new record ->
   * c1 -> { 1, 2 }
   * c2 -> { 3, 3 }     CAN JOIN
   * c3 -> { 4, 5 }
   *
   *    * c1 -> { 1, 2 }
   *    * c2 -> { 2, 3 }     NO JOIN
   *    * c3 -> { 4, 5 }
   */
  private fun List<PersonKeyEntity>.containsExcluded(): Boolean {
    val map = this.map { it.personUUID to it.getScopes() }.associate { it.first to it.second }

    val seen = mutableSetOf<UUID>()
    return map.values.any { scopes ->
      val x = scopes.toSet()
      x.any { !seen.add(it) }
    }

//    this.forEach { cluster ->
//      val scopes = cluster.getScopes()
//
//
//    }
  }
}
