package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.TPRNTS
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.UNKN
import kotlin.collections.plus

val PROBATION_RELIGION_CODE_MAPPING: Map<String, ReligionCode> = ReligionCode.entries.associateBy { it.name }
  .plus("-1" to UNKN)
  .plus("REL01" to TPRNTS)
