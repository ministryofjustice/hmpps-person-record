package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideScopeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OverrideScopeRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ActorType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ConfidenceType
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class OverrideService(
  private val overrideScopeRepository: OverrideScopeRepository,
  private val personMatchService: PersonMatchService,
) {

  fun systemExclude(vararg records: PersonEntity) {
    val scopeEntity = createScope()
    records.forEach {
      it.addOverrideMarker(scopeEntity)
      personMatchService.saveToPersonMatch(it)
    }
  }

  fun systemInclude(vararg records: PersonEntity) {
    val marker = OverrideScopeEntity.newMarker()
    val scopeEntity = createScope()
    records.forEach {
      it.addOverrideMarker(scopeEntity, marker)
      personMatchService.saveToPersonMatch(it)
    }
  }

  private fun createScope(): OverrideScopeEntity = overrideScopeRepository.save(
    OverrideScopeEntity.new(ConfidenceType.VERIFIED, ActorType.SYSTEM),
  )
}
