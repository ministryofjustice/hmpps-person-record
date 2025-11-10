package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
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
) {
  companion object {

    fun from(disabilityStatus: PrisonDisabilityStatus): PrisonDisabilityStatusEntity = PrisonDisabilityStatusEntity(
      cprDisabilityStatusId = UUID.randomUUID(),
      prisonNumber = disabilityStatus.prisonNumber,
    )
  }
}
