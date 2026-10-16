package uk.gov.justice.digital.hmpps.personrecord.service.message

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

interface PersonGranularDataMerger {
  fun handleMerge(fromPerson: PersonEntity, toPerson: PersonEntity)
}
