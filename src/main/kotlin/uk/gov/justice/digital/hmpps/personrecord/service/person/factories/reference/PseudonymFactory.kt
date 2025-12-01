package uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class PseudonymFactory {
  fun buildPseudonyms(person: Person): List<PseudonymEntity> {
    val primaryName = person.buildPrimaryName()
    val aliases = person.aliases.mapNotNull { it.buildAlias() }
    return listOf(primaryName) + aliases
  }

  private fun Person.buildPrimaryName(): PseudonymEntity = PseudonymEntity.primaryNameFrom(this)

  private fun Alias.buildAlias(): PseudonymEntity? = PseudonymEntity.aliasFrom(this)
}
