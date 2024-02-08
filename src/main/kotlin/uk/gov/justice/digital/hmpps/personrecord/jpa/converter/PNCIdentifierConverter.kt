package uk.gov.justice.digital.hmpps.personrecord.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier

@Converter(autoApply = true)
class PNCIdentifierConverter : AttributeConverter<PNCIdentifier, String> {

  override fun convertToDatabaseColumn(pncIdentifier: PNCIdentifier?): String? {
    return pncIdentifier?.pncId
  }

  override fun convertToEntityAttribute(pncIdentifier: String?): PNCIdentifier {
    return PNCIdentifier.from(pncIdentifier)
  }
}
