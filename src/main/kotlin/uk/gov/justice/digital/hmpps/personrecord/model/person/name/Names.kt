package uk.gov.justice.digital.hmpps.personrecord.model.person.name

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NameEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType

data class Names(
  val preferred: Name? = null,
  val aliases: List<Name> = emptyList(),
  val nickname: Name? = null,
  val maiden: Name? = null,
) {
  fun build(): MutableList<NameEntity> {
    val names: List<Name> = listOfNotNull(preferred, nickname, maiden) + aliases
    return NameEntity.fromList(names).toMutableList()
  }

  companion object {
    fun from(names: MutableList<NameEntity>): Names {
      return Names(
        preferred = convert(names.filter { it.type == NameType.PREFERRED }).getOrNull(0),
        aliases = convert(names.filter { it.type == NameType.ALIAS }),
        nickname = convert(names.filter { it.type == NameType.NICKNAME }).getOrNull(0),
        maiden = convert(names.filter { it.type == NameType.MAIDEN }).getOrNull(0),
      )
    }

    private fun convert(nameEntities: List<NameEntity>): List<Name> {
      return nameEntities.map {
        Name(
          title = it.title,
          firstName = it.firstName,
          middleNames = it.middleNames,
          lastName = it.lastName,
          dateOfBirth = it.dateOfBirth,
          type = it.type,
        )
      }
    }
  }
}
