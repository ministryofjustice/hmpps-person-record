package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object ReferenceBuilder {

  fun buildReferences(person: Person): List<ReferenceEntity> = person.references
    .filterNot { it.identifierValue.isNullOrEmpty() }
    .map { ReferenceEntity.from(it) }
}
