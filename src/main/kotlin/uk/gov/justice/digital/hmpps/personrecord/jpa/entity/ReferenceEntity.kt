package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

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
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "reference")
class ReferenceEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(
    name = "update_id",
    insertable = false,
    updatable = false,
    nullable = false,
  )
  @Generated
  var updateId: UUID? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column(name = "identifier_type")
  @Enumerated(STRING)
  val identifierType: IdentifierType,

  @Column(name = "identifier_value")
  val identifierValue: String? = null,

  @Column(name = "identifier_comment")
  val comment: String? = null,

  @Column(name = "is_verified")
  var isVerified: Boolean? = null,

  @Column(name = "create_date_time")
  var createDateTime: LocalDateTime? = null,

  @Column(name = "create_user_id")
  var createUserId: String? = null,

  @Column(name = "modify_date_time")
  var modifyDateTime: LocalDateTime? = null,

  @Column(name = "modify_user_id")
  var modifyUserId: String? = null,

  @Version
  var version: Int = 0,
) {

  companion object {
    fun from(reference: Reference): ReferenceEntity = ReferenceEntity(
      identifierType = reference.identifierType,
      identifierValue = reference.identifierValue,
      comment = reference.comment,
    )
  }
}
