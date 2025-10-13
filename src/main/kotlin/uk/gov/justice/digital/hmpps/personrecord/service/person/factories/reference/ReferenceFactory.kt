package uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

class ReferenceFactory {

  fun buildReferences(person: Person, existingReferences: List<ReferenceEntity>): List<ReferenceEntity> = ReferenceEntity.fromList(person.references).map { reference ->
    reference.existsIn(
      references = existingReferences,
      yes = { it },
      no = { reference },
    )
  }

  private fun ReferenceEntity.existsIn(references: List<ReferenceEntity>, yes: (ReferenceEntity) -> ReferenceEntity, no: () -> ReferenceEntity): ReferenceEntity {
    val referenceEntity = references.find { it.identifierType == this.identifierType && it.identifierValue == this.identifierValue }
    return when {
      referenceEntity != null -> yes(referenceEntity)
      else -> no()
    }
  }
}
