package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.PersonSearchRequest

interface PersonRepositoryCustom {
  fun searchByRequestParameters(personSearchRequest: PersonSearchRequest): List<PersonEntity>
}
