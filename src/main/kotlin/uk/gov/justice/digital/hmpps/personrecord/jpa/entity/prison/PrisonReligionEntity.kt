package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "prison_religion")
class PrisonReligionEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "cpr_religion_id", nullable = false)
  val cprReligionId: UUID,

  @Column(name = "religion_code")
  var code: String? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @Column(name = "change_reason_known")
  var changeReasonKnown: Boolean? = null,

  @Column
  var comments: String? = null,

  @Column
  var verified: Boolean? = null,

  @Column(name = "start_date")
  var startDate: LocalDate? = null,

  @Column(name = "end_date")
  var endDate: LocalDate? = null,

  @Column(name = "modify_date_time")
  var modifyDateTime: LocalDateTime? = null,

  @Column(name = "modify_user_id")
  var modifyUserId: String? = null,

  @Column(name = "record_type")
  @Enumerated(STRING)
  var prisonRecordType: PrisonRecordType? = null,

) {

  fun update(prisonReligion: PrisonReligion) {
    this.code = prisonReligion.religionCode
    this.startDate = prisonReligion.startDate
    this.endDate = prisonReligion.endDate
    this.modifyUserId = prisonReligion.modifyUserId
    this.modifyDateTime = prisonReligion.modifyDateTime
    this.prisonRecordType = PrisonRecordType.from(prisonReligion.current)
    this.comments = prisonReligion.comments
    this.changeReasonKnown = prisonReligion.changeReasonKnown
    this.verified = prisonReligion.verified
  }

  companion object {
    fun from(prisonNumber: String, prisonReligion: PrisonReligion) = PrisonReligionEntity(
      cprReligionId = UUID.randomUUID(),
      prisonNumber = prisonNumber,
      code = prisonReligion.religionCode,
      changeReasonKnown = prisonReligion.changeReasonKnown,
      comments = prisonReligion.comments,
      verified = prisonReligion.verified,
      startDate = prisonReligion.startDate,
      endDate = prisonReligion.endDate,
      modifyDateTime = prisonReligion.modifyDateTime,
      modifyUserId = prisonReligion.modifyUserId,
      prisonRecordType = PrisonRecordType.from(prisonReligion.current),
    )
  }
}
