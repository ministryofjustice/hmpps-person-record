package uk.gov.justice.digital.hmpps.personrecord.service.refdata

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.refdata.ReferenceDataValue
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataType

@Service
class ReferenceDataService {

  fun getAllReferenceData(): Map<String, List<ReferenceDataValue>> = ReferenceDataType.entries.associate { it.key to it.toValues() }

  fun getReferenceData(type: String): Map<String, List<ReferenceDataValue>> {
    val referenceDataType = ReferenceDataType.fromKey(type) ?: throw ResourceNotFoundException(type)
    return mapOf(referenceDataType.key to referenceDataType.toValues())
  }

  private fun ReferenceDataType.toValues(): List<ReferenceDataValue> = values.map { ReferenceDataValue.from(it) }
}
