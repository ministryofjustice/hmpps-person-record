package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference

object ReferenceBuilder {

  fun buildReferences(person: Person, personEntity: PersonEntity): List<ReferenceEntity> = person.references
    .filterNot { it.identifierValue.isNullOrEmpty() }
    .mapNotNull { reference ->
      reference.existsIn(
        childEntities = personEntity.references,
        match = { ref, entity -> ref.matches(entity) },
        yes = { it },
        no = { ReferenceEntity.from(reference) },
      )
    }

  private fun Reference.matches(entity: ReferenceEntity): Boolean = this == Reference.from(entity)
}
