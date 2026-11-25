package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.review.ReviewRaised
import uk.gov.justice.digital.hmpps.personrecord.service.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonKeyService(
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  fun linkRecordToPersonKey(personEntity: PersonEntity) {
    val clusters = personMatchService.findClustersToJoin(personEntity) // delete this as well
    when {
      clusters.containsExcludeOverrides() -> personEntity.setAsOverrideConflict(clusters)
      else -> {
        assignPersonToNewPersonKey(personEntity)
        reclusterService.recluster(personEntity)
      }
    }
  }

  private fun assignPersonToNewPersonKey(personEntity: PersonEntity) {
    val personKey = PersonKeyEntity.new()
    publisher.publishEvent(PersonKeyCreated(personEntity, personKey))
    personEntity.assignToPersonKey(personKey)
    personKeyRepository.save(personKey)
  }

  private fun PersonEntity.setAsOverrideConflict(matchingClusters: List<PersonKeyEntity>) {
    assignPersonToNewPersonKey(this)
    this.personKey?.let {
      it.setAsNeedsAttention(OVERRIDE_CONFLICT)
      publisher.publishEvent(ReviewRaised(it, matchingClusters))
    }
  }

  private fun List<PersonKeyEntity>.containsExcludeOverrides(): Boolean {
    this.forEach { cluster ->
      val thisClusterScopes = cluster.getExcludeScopes()
      val otherClusters = this.filter { it != cluster }
      otherClusters.forEach { otherCluster ->
        val otherClusterScopes = otherCluster.getExcludeScopes()
        if (otherClusterScopes.any { thisClusterScopes.contains(it) }) {
          return true
        }
      }
    }
    return false
  }
}
