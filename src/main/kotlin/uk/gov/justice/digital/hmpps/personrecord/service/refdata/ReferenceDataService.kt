package uk.gov.justice.digital.hmpps.personrecord.service.refdata

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.refdata.ReferenceDataValue
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataType

@Service
class ReferenceDataService {

  fun getAllReferenceData(applicableTo: ReferenceDataDomain? = null): Map<String, List<ReferenceDataValue>> = ReferenceDataType.entries.associate { it.key to it.toValues(applicableTo) }

  fun getReferenceData(type: String, applicableTo: ReferenceDataDomain? = null): Map<String, List<ReferenceDataValue>> {
    val referenceDataType = ReferenceDataType.fromKey(type) ?: throw ResourceNotFoundException(type)
    return mapOf(referenceDataType.key to referenceDataType.toValues(applicableTo))
  }

  private fun ReferenceDataType.toValues(applicableTo: ReferenceDataDomain?): List<ReferenceDataValue> = values
    .filter { applicableTo == null || applicableTo in it.applicableTo }
    .map { ReferenceDataValue.from(it) }
}
