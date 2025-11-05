package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.model.types.RecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import java.time.LocalDate

@Entity
@Table(name = "prison_sexual_orientation")
class PrisonSexualOrientationEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @Column(name = "sexual_orientation_code")
  @Enumerated(STRING)
  val sexualOrientationCode: SexualOrientation,

  @Column(name = "start_date")
  val startDate: LocalDate? = null,

  @Column(name = "end_date")
  val endDate: LocalDate? = null,

  @Column(name = "record_type")
  @Enumerated(STRING)
  val recordType: RecordType,
)
