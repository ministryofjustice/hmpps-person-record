package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "prison_religion")
class PrisonReligionEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "religion_code", nullable = false)
  val code: String,

  @Column
  val status: String? = null,

  @Column(name = "change_reason_known")
  val changeReasonKnown: String? = null,

  @Column
  val comments: String? = null,

  @Column
  val verified: Boolean? = null,

  @Column(name = "start_date")
  var startDate: LocalDate? = null,

  @Column(name = "end_date")
  var endDate: LocalDate? = null,

  @Column(name = "create_user_id")
  var createUserId: String? = null,

  @Column(name = "create_date_time")
  var createDateTime: LocalDateTime? = null,

  @Column(name = "create_display_name")
  var createDisplayName: String? = null,

  @Column(name = "modify_date_time")
  var modifyDateTime: LocalDateTime? = null,

  @Column(name = "modify_user_id")
  var modifyUserId: String? = null,

  @Column(name = "modify_display_name")
  var modifyDisplayName: String? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity,

)
