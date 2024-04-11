package uk.gov.justice.digital.hmpps.personrecord.jpa.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier

@Converter(autoApply = true)
class CROIdentifierConverter : AttributeConverter<CROIdentifier, String> {

  override fun convertToDatabaseColumn(croIdentifier: CROIdentifier?): String? {
    return croIdentifier?.croId
  }

  override fun convertToEntityAttribute(croIdentifier: String?): CROIdentifier {
    return CROIdentifier.from(croIdentifier)
  }
}
