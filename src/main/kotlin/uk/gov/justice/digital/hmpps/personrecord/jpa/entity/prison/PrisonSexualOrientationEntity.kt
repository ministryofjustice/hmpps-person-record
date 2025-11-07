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
  val sexualOrientationCode: SexualOrientation,

  @Column(name = "start_date")
  val startDate: LocalDate? = null,

  @Column(name = "end_date")
  val endDate: LocalDate? = null,

  @Column(name = "create_user_id")
  val createUserId: String? = null,

  @Column(name = "create_date_time")
  val createDateTime: LocalDateTime? = null,

  @Column(name = "create_display_name")
  val createDisplayName: String? = null,

  @Column(name = "modify_date_time")
  val modifyDateTime: LocalDateTime? = null,

  @Column(name = "modify_user_id")
  val modifyUserId: String? = null,

  @Column(name = "modify_display_name")
  val modifyDisplayName: String? = null,

  @Column(name = "record_type")
  @Enumerated(STRING)
  val prisonRecordType: PrisonRecordType? = null,

) {
  companion object {

    fun from(sexualOrientation: PrisonSexualOrientation): PrisonSexualOrientationEntity = PrisonSexualOrientationEntity(
      cprSexualOrientationId = UUID.randomUUID(),
      prisonNumber = sexualOrientation.prisonNumber,
      sexualOrientationCode = SexualOrientation.from(sexualOrientation),
      startDate = sexualOrientation.startDate,
      endDate = sexualOrientation.endDate,
      createUserId = sexualOrientation.createUserId,
      createDateTime = sexualOrientation.createDateTime,
      createDisplayName = sexualOrientation.createDisplayName,
      modifyDateTime = sexualOrientation.modifyDateTime,
      modifyUserId = sexualOrientation.modifyUserId,
      modifyDisplayName = sexualOrientation.modifyDisplayName,
      prisonRecordType = when (sexualOrientation.current) {
        true -> PrisonRecordType.CURRENT
        false -> PrisonRecordType.HISTORIC
      },
    )
  }
}
