package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.NationalityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Nationality
import java.time.LocalDate

@Entity
@Table(name = "nationalities")
class NationalityEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_nationality_code_id",
    referencedColumnName = "id",
  )
  var nationalityCode: NationalityCodeEntity? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column(name = "start_date")
  val startDate: LocalDate? = null,

  @Column(name = "end_date")
  val endDate: LocalDate? = null,

  @Column(name = "notes")
  val notes: String? = null,
) {
  companion object {

    fun from(nationality: Nationality, nationalityCode: NationalityCodeEntity?): NationalityEntity? = nationalityCode?.let {
      NationalityEntity(
        nationalityCode = nationalityCode,
        startDate = nationality.startDate,
        endDate = nationality.endDate,
        notes = nationality.notes,
      )
    }
  }
}
