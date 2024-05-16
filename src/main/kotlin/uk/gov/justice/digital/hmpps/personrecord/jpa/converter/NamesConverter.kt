package uk.gov.justice.digital.hmpps.personrecord.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NameEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.name.Names

@Converter(autoApply = true)
class NamesConverter : AttributeConverter<Names, MutableList<NameEntity>> {

  override fun convertToDatabaseColumn(names: Names): MutableList<NameEntity> {
    return NameEntity.fromList(names.build()).toMutableList()
  }

  override fun convertToEntityAttribute(names: MutableList<NameEntity>): Names {
    return Names.from(names)
  }
}
