package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType

object PseudonymBuilder {

  fun buildPseudonyms(person: Person, personEntity: PersonEntity): List<PseudonymEntity> {
    val pseudonyms = (listOf(person.currentAlias()) + person.aliases).mapIndexedNotNull { index, alias ->
      val nameType = if (index == 0) NameType.PRIMARY else NameType.ALIAS
      alias.existsIn(
        childEntities = personEntity.pseudonyms,
        match = { ref, entity -> entity.matches(ref, nameType) },
        yes = { it },
        no = { alias.from(nameType) },
      )
    }
    return pseudonyms
  }
}

private fun Alias.from(nameType: NameType): PseudonymEntity = PseudonymEntity(
  firstName = this.firstName,
  middleNames = this.middleNames,
  lastName = this.lastName,
  nameType = nameType,
  titleCode = this.titleCode,
  dateOfBirth = this.dateOfBirth,
  sexCode = this.sexCode,
)

private fun PseudonymEntity.matches(alias: Alias, nameType: NameType): Boolean = alias == Alias.from(this) && this.nameType == nameType

private fun Person.currentAlias() = Alias(
  firstName = firstName,
  lastName = lastName,
  middleNames = middleNames,
  titleCode = titleCode,
  dateOfBirth = dateOfBirth,
  sexCode = sexCode,
)
