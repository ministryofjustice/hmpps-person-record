package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "court_message")
class CourtMessageEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "message_id")
  val messageId: String? = null,

  @Column(name = "message")
  var message: String? = null,

  @Version
  var version: Int = 0,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_hearing_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var courtHearing: CourtHearingEntity? = null,

)
