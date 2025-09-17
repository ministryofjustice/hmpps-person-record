package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideScopeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OverrideScopeRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ActorType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ConfidenceType

@Component
class OverrideService(
  private val overrideScopeRepository: OverrideScopeRepository,
) {

  fun systemExclude(vararg records: PersonEntity) {
    val scopeEntity = createScope()
    records.forEach {
      it.addOverrideMarker(OverrideScopeEntity.newMarker(), scopeEntity)
    }
  }

  fun systemInclude(vararg records: PersonEntity) {
    val marker = OverrideScopeEntity.newMarker()
    val scopeEntity = createScope()
    records.forEach {
      it.addOverrideMarker(marker, scopeEntity)
    }
  }

  private fun createScope(): OverrideScopeEntity = overrideScopeRepository.save(
    OverrideScopeEntity.new(ConfidenceType.VERIFIED, ActorType.SYSTEM),
  )
}
