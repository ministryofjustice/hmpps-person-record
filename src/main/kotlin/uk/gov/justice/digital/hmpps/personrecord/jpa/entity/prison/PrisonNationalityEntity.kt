package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationality
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "prison_nationalities")
class PrisonNationalityEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "cpr_nationality_id", nullable = false)
  val cprNationalityId: UUID,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @Column(name = "nationality_code")
  @Enumerated(STRING)
  var nationalityCode: NationalityCode,

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

  @Column(name = "nationality_notes")
  var notes: String? = null,

) {

  companion object {

    fun from(prisonNationality: PrisonNationality): PrisonNationalityEntity = PrisonNationalityEntity(
      cprNationalityId = UUID.randomUUID(),
      prisonNumber = prisonNationality.prisonNumber,
      nationalityCode = NationalityCode.fromPrisonMapping(prisonNationality.nationalityCode)!!,
      startDate = prisonNationality.startDate,
      endDate = prisonNationality.endDate,
      createUserId = prisonNationality.createUserId,
      createDateTime = prisonNationality.createDateTime,
      createDisplayName = prisonNationality.createDisplayName,
      modifyDateTime = prisonNationality.modifyDateTime,
      modifyUserId = prisonNationality.modifyUserId,
      modifyDisplayName = prisonNationality.modifyDisplayName,
      prisonRecordType = PrisonRecordType.from(prisonNationality.current),
      notes = prisonNationality.notes,
    )
  }
}
