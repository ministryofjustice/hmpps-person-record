package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType

object PseudonymBuilder {

  fun buildPseudonyms(person: Person, personEntity: PersonEntity): List<PseudonymEntity> {
    // We need to allow for semantically identical pseudonyms at present.
    // Therefore, we need to check that we don't match the same pseudonym multiple times.
    val alreadyMatchedIds = mutableSetOf<Long>()
    val allPseudonyms = listOf(person.primaryAlias()) + person.aliases
    return allPseudonyms.mapIndexedNotNull { index, pseudonym ->
      val nameType = if (index == 0) NameType.PRIMARY else NameType.ALIAS
      pseudonym.existsIn(
        childEntities = personEntity.pseudonyms,
        match = { ref, entity -> entity.matches(ref, nameType, alreadyMatchedIds) },
        yes = { it },
        no = { pseudonym.from(nameType) },
      )
    }
  }
}

private fun Alias.from(nameType: NameType): PseudonymEntity? = when {
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
  matchedPseudonymIds: MutableSet<Long>,
): Boolean {
  if (id == null ||
    id in matchedPseudonymIds ||
    this.nameType != nameType ||
    alias != Alias.from(this)
  ) {
    return false
  }
  matchedPseudonymIds += id!!
  return true
}

private fun isAliasPresent(firstName: String?, middleNames: String?, surname: String?): Boolean = sequenceOf(firstName, middleNames, surname)
  .filterNotNull().any { it.isNotBlank() }
