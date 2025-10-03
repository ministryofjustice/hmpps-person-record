package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.MERGED
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import java.util.UUID

@Entity
@Table(name = "person_key")
class PersonKeyEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "person_uuid")
  val personUUID: UUID? = null,

  @Column
  @OneToMany(mappedBy = "personKey", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var personEntities: MutableList<PersonEntity> = mutableListOf(),

  @Column(name = "merged_to")
  var mergedTo: Long? = null,

  @Column
  @Enumerated(STRING)
  var status: UUIDStatusType = ACTIVE,

  @Column
  @Enumerated(STRING)
  var statusReason: UUIDStatusReasonType? = null,

  @Version
  var version: Int = 0,

) {

  fun isActive(): Boolean = status == ACTIVE

  fun isNotOverrideConflict(): Boolean = status == NEEDS_ATTENTION && statusReason != OVERRIDE_CONFLICT

  fun setAsActive() {
    this.apply {
      this.status = ACTIVE
      this.statusReason = null
    }
  }

  fun setAsNeedsAttention(reason: UUIDStatusReasonType) {
    this.apply {
      this.status = NEEDS_ATTENTION
      this.statusReason = reason
    }
  }

  fun markAsMerged(to: PersonKeyEntity) {
    this.apply {
      this.mergedTo = to.id
      this.status = MERGED
    }
  }

  companion object {
    val empty: PersonKeyEntity? = null

    fun new(): PersonKeyEntity = PersonKeyEntity(personUUID = UUID.randomUUID())
  }
}
