package uk.gov.justice.digital.hmpps.personrecord.service.person.factory

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.mixin.CreateMixin
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.mixin.SearchMixin
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.mixin.UpdateMixin

class PersonProcessorChain(
  override val context: PersonContext,
  override val personRepository: PersonRepository,
): CreateMixin, SearchMixin, UpdateMixin {

  fun result(): PersonEntity? = context.personEntity

}