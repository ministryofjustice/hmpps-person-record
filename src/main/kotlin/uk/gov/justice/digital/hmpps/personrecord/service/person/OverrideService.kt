package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideScopeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OverrideScopeRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ActorType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ConfidenceType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.OverrideType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.OverrideType.EXCLUDE
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.OverrideType.INCLUDE
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class OverrideService(
  private val overrideScopeRepository: OverrideScopeRepository,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
) {

  fun systemExclude(vararg records: PersonEntity) {
    val scopeEntity = createScope(EXCLUDE)
    records.forEach {
      it.addOverrideMarker(scopeEntity)
      personMatchService.saveToPersonMatch(it)
    }
  }

  fun systemInclude(recordOne: PersonEntity, recordTwo: PersonEntity) {
    if (recordsNotIncluded(recordOne, recordTwo)) {
      val marker = OverrideScopeEntity.newMarker()
      val scopeEntity = createScope(INCLUDE)
      listOf(recordOne, recordTwo).forEach {
        it.addOverrideMarker(scopeEntity, marker)
        personMatchService.saveToPersonMatch(it)
      }
      reclusterService.recluster(recordOne)
    }
  }

  private fun createScope(overrideType: OverrideType): OverrideScopeEntity = overrideScopeRepository.save(
    OverrideScopeEntity.new(ConfidenceType.VERIFIED, ActorType.SYSTEM, overrideType),
  )

  private fun recordsNotIncluded(recordOne: PersonEntity, recordTwo: PersonEntity): Boolean = recordOne.getIncludeScopes().intersect(recordTwo.getIncludeScopes()).isEmpty()
}
