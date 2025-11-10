package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatus
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "prison_immigration_status")
class PrisonImmigrationStatusEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "cpr_immigration_status_id", nullable = false)
  val cprImmigrationStatusId: UUID,

  @Column(name = "interest_to_immigration")
  var interestToImmigration: Boolean,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

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

  fun update(immigrationStatus: PrisonImmigrationStatus) {
    this.interestToImmigration = immigrationStatus.interestToImmigration
    this.startDate = immigrationStatus.startDate
    this.endDate = immigrationStatus.endDate
    this.createUserId = immigrationStatus.createUserId
    this.createDateTime = immigrationStatus.createDateTime
    this.createDisplayName = immigrationStatus.createDisplayName
    this.modifyUserId = immigrationStatus.modifyUserId
    this.modifyDisplayName = immigrationStatus.modifyDisplayName
    this.modifyDateTime = immigrationStatus.modifyDateTime
    this.prisonRecordType = getRecordType(immigrationStatus)
  }

  companion object {
    fun from(immigrationStatus: PrisonImmigrationStatus) = PrisonImmigrationStatusEntity(
      cprImmigrationStatusId = UUID.randomUUID(),
      interestToImmigration = immigrationStatus.interestToImmigration,
      prisonNumber = immigrationStatus.prisonNumber,
      startDate = immigrationStatus.startDate,
      endDate = immigrationStatus.endDate,
      createUserId = immigrationStatus.createUserId,
      createDateTime = immigrationStatus.createDateTime,
      createDisplayName = immigrationStatus.createDisplayName,
      modifyDateTime = immigrationStatus.modifyDateTime,
      modifyUserId = immigrationStatus.modifyUserId,
      modifyDisplayName = immigrationStatus.modifyDisplayName,
      prisonRecordType = getRecordType(immigrationStatus),
    )

    private fun getRecordType(immigrationStatus: PrisonImmigrationStatus): PrisonRecordType = when (immigrationStatus.current) {
      true -> PrisonRecordType.CURRENT
      false -> PrisonRecordType.HISTORIC
    }
  }
}
