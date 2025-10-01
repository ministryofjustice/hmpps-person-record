package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.admin

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import java.util.UUID

@Entity
@Table(name = "person_key")
class AdminClusterEntity(
  @Id
  val id: Long? = null,

  @Column(name = "person_uuid")
  val personUUID: UUID? = null,

  @Column
  @OneToMany(mappedBy = "personKey", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var adminPersonEntities: MutableList<AdminPersonEntity> = mutableListOf(),

  @Column(name = "merged_to")
  var mergedTo: Long? = null,

  @Column
  @Enumerated(STRING)
  var status: UUIDStatusType = UUIDStatusType.ACTIVE,

  @Column
  @Enumerated(STRING)
  var statusReason: UUIDStatusReasonType? = null,

  @Version
  var version: Int = 0,

)
