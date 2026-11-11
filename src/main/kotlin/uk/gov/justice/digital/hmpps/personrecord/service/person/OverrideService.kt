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
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class OverrideService(
  private val overrideScopeRepository: OverrideScopeRepository,
  private val personMatchService: PersonMatchService,
  private val personService: PersonService,
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
      val scopeEntity = createScope(INCLUDE)
      val marker = OverrideScopeEntity.newMarker()

      personService.processUpdate(recordOne) { personEntity, _ ->
        personEntity.addOverrideMarker(scopeEntity, marker)
      }
      personService.processUpdate(recordTwo) { personEntity, _ ->
        personEntity.addOverrideMarker(scopeEntity, marker)
      }
    }
  }

  private fun createScope(overrideType: OverrideType): OverrideScopeEntity = overrideScopeRepository.save(
    OverrideScopeEntity.new(ConfidenceType.VERIFIED, ActorType.SYSTEM, overrideType),
  )

  private fun recordsNotIncluded(recordOne: PersonEntity, recordTwo: PersonEntity): Boolean = recordOne.getIncludeScopes().intersect(recordTwo.getIncludeScopes()).isEmpty()
}
