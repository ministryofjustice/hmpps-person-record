package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType

@Entity
@Table(name = "override_marker")
class OverrideMarkerEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column(name = "marker_type")
  @Enumerated(EnumType.STRING)
  val markerType: OverrideMarkerType,

  @Column(name = "marker_value")
  val markerValue: Long? = null,

  @Version
  var version: Int = 0,

  @Column(name = "fk_person_id", insertable = false, updatable = false)
  val fkPersonId: Long? = null,
)
