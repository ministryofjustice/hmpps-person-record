package uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class NationalityFactory {
  // TODO simplify - will not have to handle empty nationalities
  fun buildNationalities(person: Person): List<NationalityEntity> = person.nationalities.mapNotNull { NationalityEntity.from(it) }
}
