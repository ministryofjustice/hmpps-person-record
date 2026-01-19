package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode

@Entity
@Table(name = "nationalities")
class NationalityEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column(name = "nationality_code")
  @Enumerated(EnumType.STRING)
  var nationalityCode: NationalityCode?,
) {
  companion object {

    fun from(nationalityCode: NationalityCode?): NationalityEntity = NationalityEntity(
      nationalityCode = nationalityCode,
    )
  }
}
