package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ActorType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ConfidenceType
import java.util.UUID

@Entity
@Table(name = "override_scopes")
class OverrideScopeEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToMany(mappedBy = "overrideScopes")
  var person: MutableList<PersonEntity> = mutableListOf(),

  @Column
  val scope: UUID,

  @Column
  @Enumerated(EnumType.STRING)
  val confidence: ConfidenceType,

  @Column
  @Enumerated(EnumType.STRING)
  val actor: ActorType,

  @Version
  var version: Int = 0,
)
