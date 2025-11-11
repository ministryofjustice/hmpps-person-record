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
  var disability: Boolean,

  @Column(name = "record_type")
  @Enumerated(STRING)
  var prisonRecordType: PrisonRecordType? = null,

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
) {

  fun update(disabilityStatus: PrisonDisabilityStatus) {
    this.disability = disabilityStatus.disability
    this.startDate = disabilityStatus.startDate
    this.endDate = disabilityStatus.endDate
    this.createUserId = disabilityStatus.createUserId
    this.createDateTime = disabilityStatus.createDateTime
    this.createDisplayName = disabilityStatus.createDisplayName
    this.modifyUserId = disabilityStatus.modifyUserId
    this.modifyDisplayName = disabilityStatus.modifyDisplayName
    this.modifyDateTime = disabilityStatus.modifyDateTime
    this.prisonRecordType = PrisonRecordType.from(disabilityStatus.current)
  }

  companion object {

    fun from(disabilityStatus: PrisonDisabilityStatus): PrisonDisabilityStatusEntity = PrisonDisabilityStatusEntity(
      cprDisabilityStatusId = UUID.randomUUID(),
      prisonNumber = disabilityStatus.prisonNumber,
      disability = disabilityStatus.disability,
      prisonRecordType = PrisonRecordType.from(disabilityStatus.current),
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
