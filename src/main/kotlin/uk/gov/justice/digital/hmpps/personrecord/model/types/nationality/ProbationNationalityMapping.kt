package uk.gov.justice.digital.hmpps.personrecord.model.types.nationality

import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.HKG
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode.KOS

val PROBATION_NATIONALITY_MAPPING: Map<String, NationalityCode> = NationalityCode.entries.associateBy { it.name }
  .plus("HK" to HKG)
  .plus("KSV" to KOS)
