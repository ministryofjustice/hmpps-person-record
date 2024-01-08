package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import java.time.LocalDate

@Entity
@Table(name = "prisoner")
@Audited
class PrisonerEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "offender_id")
  val offenderId: String? = null,

  @Column(name = "pnc_number")
  val pncNumber: String? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

) : BaseAuditedEntity() {
  companion object {
    fun from(prisoner: Prisoner): PrisonerEntity {
      val prisonerEntity =
        PrisonerEntity(
          offenderId = prisoner.prisonerNumber,
          pncNumber = prisoner.pncNumber,
          firstName = prisoner.firstName,
          lastName = prisoner.lastName,
          dateOfBirth = prisoner.dateOfBirth,
        ).also {
          it.createdBy = PERSON_RECORD_SERVICE
          it.lastUpdatedBy = PERSON_RECORD_SERVICE
        }
      return prisonerEntity
    }
  }
}
