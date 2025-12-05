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
import java.time.LocalDateTime

@Entity
@Table(name = "prison_nationalities")
class PrisonNationalityEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @Column(name = "nationality_code")
  @Enumerated(STRING)
  var nationalityCode: NationalityCode? = null,

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

  fun update(prisonNationality: PrisonNationality) {
    this.nationalityCode = prisonNationality.nationalityCode?.let { NationalityCode.fromPrisonMapping(prisonNationality.nationalityCode) }
    this.modifyUserId = prisonNationality.modifyUserId
    this.modifyDisplayName = prisonNationality.modifyDisplayName
    this.modifyDateTime = prisonNationality.modifyDateTime
    this.prisonRecordType = PrisonRecordType.CURRENT
    this.notes = prisonNationality.notes
  }

  companion object {

    fun from(prisonNumber: String, prisonNationality: PrisonNationality): PrisonNationalityEntity = PrisonNationalityEntity(
      prisonNumber = prisonNumber,
      nationalityCode = prisonNationality.nationalityCode?.let { NationalityCode.fromPrisonMapping(prisonNationality.nationalityCode) },
      modifyDateTime = prisonNationality.modifyDateTime,
      modifyUserId = prisonNationality.modifyUserId,
      modifyDisplayName = prisonNationality.modifyDisplayName,
      prisonRecordType = PrisonRecordType.CURRENT,
      notes = prisonNationality.notes,
    )
  }
}
