package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

@Entity
@Table(name = "reference")
class ReferenceEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  @JsonBackReference
  var person: PersonEntity? = null,

  @Column(name = "identifier_type")
  @Enumerated(STRING)
  val identifierType: IdentifierType,

  @Column(name = "identifier_value")
  val identifierValue: String? = null,

  @Version
  @JsonIgnore
  var version: Int = 0,
) {
  companion object {
    private fun from(reference: Reference): ReferenceEntity {
      return ReferenceEntity(identifierType = reference.identifierType, identifierValue = reference.identifierValue)
    }

    fun fromList(references: List<Reference>): List<ReferenceEntity> {
      return references.filterNot { it.identifierValue.isNullOrEmpty() }.map { from(it) }
    }
  }
}
