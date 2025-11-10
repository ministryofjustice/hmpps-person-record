package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "prison_disability_status")
class PrisonDisabilityStatusEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "cpr_disability_status_id", nullable = false)
  val cprDisabilityStatusId: UUID,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @Column(name = "disability")
  val disability: Boolean,

  @Column(name = "record_type")
  @Enumerated(STRING)
  val prisonRecordType: PrisonRecordType? = null,

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
) {
  companion object {

    fun from(disabilityStatus: PrisonDisabilityStatus): PrisonDisabilityStatusEntity = PrisonDisabilityStatusEntity(
      cprDisabilityStatusId = UUID.randomUUID(),
      prisonNumber = disabilityStatus.prisonNumber,
      disability = disabilityStatus.disability,
      prisonRecordType = when (disabilityStatus.current) {
        true -> PrisonRecordType.CURRENT
        false -> PrisonRecordType.HISTORIC
      },
      startDate = disabilityStatus.startDate,
      endDate = disabilityStatus.endDate,
      createUserId = disabilityStatus.createUserId,
      createDateTime = disabilityStatus.createDateTime,
      createDisplayName = disabilityStatus.createDisplayName,
      modifyDateTime = disabilityStatus.modifyDateTime,
      modifyUserId = disabilityStatus.modifyUserId,
      modifyDisplayName = disabilityStatus.modifyDisplayName,
      )
  }
}
