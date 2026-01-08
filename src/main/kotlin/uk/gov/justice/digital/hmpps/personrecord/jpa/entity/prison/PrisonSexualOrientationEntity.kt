package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "prison_sexual_orientation")
class PrisonSexualOrientationEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "cpr_sexual_orientation_id", nullable = false)
  val cprSexualOrientationId: UUID,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @Column(name = "sexual_orientation_code")
  @Enumerated(STRING)
  var sexualOrientationCode: SexualOrientation? = null,

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

  @Column(name = "record_type")
  @Enumerated(STRING)
  var prisonRecordType: PrisonRecordType? = null,

) {

  fun update(sexualOrientation: PrisonSexualOrientation) {
    this.sexualOrientationCode = sexualOrientation.sexualOrientationCode?.let { SexualOrientation.fromPrison(it) }
    this.modifyUserId = sexualOrientation.modifyUserId
    this.modifyDateTime = sexualOrientation.modifyDateTime
    this.prisonRecordType = PrisonRecordType.CURRENT
  }

  companion object {

    fun from(prisonNumber: String, sexualOrientation: PrisonSexualOrientation): PrisonSexualOrientationEntity = PrisonSexualOrientationEntity(
      cprSexualOrientationId = UUID.randomUUID(),
      prisonNumber = prisonNumber,
      sexualOrientationCode = sexualOrientation.sexualOrientationCode?.let { SexualOrientation.fromPrison(it) },
      modifyDateTime = sexualOrientation.modifyDateTime,
      modifyUserId = sexualOrientation.modifyUserId,
      prisonRecordType = PrisonRecordType.CURRENT,
    )
  }
}
