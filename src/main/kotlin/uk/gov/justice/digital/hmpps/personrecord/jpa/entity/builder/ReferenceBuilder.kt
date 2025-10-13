package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference

class ReferenceBuilder : ChildBuilder {

  override fun build(person: Person, personEntity: PersonEntity) {
    val builtReferences = personEntity.buildReferences(person)
    personEntity.references.clear()
    builtReferences.forEach { referenceEntity -> referenceEntity.person = personEntity }
    personEntity.references.addAll(builtReferences)
  }

  private fun PersonEntity.buildReferences(person: Person): List<ReferenceEntity> = person.references
    .filterNot { it.identifierValue.isNullOrEmpty() }
    .mapNotNull { reference ->
      reference.existsIn(
        childEntities = this.references,
        match = { ref, entity -> ref.matches(entity) },
        yes = { it },
        no = { ReferenceEntity.from(reference) },
      )
    }

  private fun Reference.matches(entity: ReferenceEntity): Boolean = this.identifierType == entity.identifierType && this.identifierValue == entity.identifierValue
}
