package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.annotation.Generated
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

  @Column(
    name = "update_id",
    insertable = false,
    updatable = false,
    nullable = false,
  )
  @Generated
  var updateId: UUID? = null,

  @Column(name = "prison_number", nullable = false)
  val prisonNumber: String,

  @Column(name = "religion_code")
  var code: String? = null,

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

  @Column(name = "modify_date_time", nullable = false)
  var modifyDateTime: LocalDateTime,

  @Column(name = "modify_user_id", nullable = false)
  var modifyUserId: String,

  @Column(name = "record_type", nullable = false)
  @Enumerated(STRING)
  var prisonRecordType: PrisonRecordType,

) {

  companion object {
    fun from(prisonNumber: String, prisonReligion: PrisonReligion) = PrisonReligionEntity(
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
