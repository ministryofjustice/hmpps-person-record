package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType

object PseudonymBuilder {

  fun buildPseudonyms(person: Person, personEntity: PersonEntity): List<PseudonymEntity> {
    // We need to allow for sematically identical pseudonyms at present.
    // Therefore, we need to check that we don't match the same pseudonym multiple times.
    val alreadyMatchedIds = personEntity.pseudonyms.mapNotNull { it.id }.associateWith { false }.toMutableMap()
    val pseudonyms = (listOf(person.currentAlias()) + person.aliases).mapIndexedNotNull { index, alias ->
      val nameType = if (index == 0) NameType.PRIMARY else NameType.ALIAS
      alias.existsIn(
        childEntities = personEntity.pseudonyms,
        match = { ref, entity -> entity.matches(ref, nameType, alreadyMatchedIds) },
        yes = { it },
        no = { alias.from(nameType) },
      )
    }
    return pseudonyms
  }
}

fun Alias.from(nameType: NameType): PseudonymEntity? = when {
  isAliasPresent(firstName, middleNames, lastName) ->
    PseudonymEntity(
      firstName = firstName,
      middleNames = middleNames,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      nameType = nameType,
      titleCode = titleCode,
      sexCode = sexCode,
    )
  else -> null
}

private fun PseudonymEntity.matches(
  alias: Alias,
  nameType: NameType,
  alreadyMatchedIds: MutableMap<Long, Boolean>,
): Boolean {
  val alreadyMatched = alreadyMatchedIds[this.id] ?: false
  if (this.id != null && !alreadyMatched && alias == Alias.from(this) && this.nameType == nameType) {
    alreadyMatchedIds[this.id!!] = true
    return true
  }
  return false
}

private fun isAliasPresent(firstName: String?, middleNames: String?, surname: String?): Boolean = sequenceOf(firstName, middleNames, surname)
  .filterNotNull().any { it.isNotBlank() }

private fun Person.currentAlias() = Alias(
  firstName = firstName,
  lastName = lastName,
  middleNames = middleNames,
  titleCode = titleCode,
  dateOfBirth = dateOfBirth,
  sexCode = sexCode,
)
