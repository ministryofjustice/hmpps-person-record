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
import jakarta.validation.ValidationException
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.time.LocalDate

@Entity
@Table(name = "defendant")
@Audited
class DefendantEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column(name = "defendant_id")
  val defendantId: String? = null,

  @Column(name = "pnc_number")
  val pncNumber: String? = null,

  @Column(name = "crn")
  val crn: String? = null,

  @Column(name = "cro")
  val cro: String? = null,

  @Column(name = "title")
  val title: String? = null,

  @Column(name = "forename_one")
  val forenameOne: String? = null,

  @Column(name = "forename_two")
  val forenameTwo: String? = null,

  @Column(name = "forename_three")
  val forenameThree: String? = null,

  @Column(name = "surname")
  val surname: String? = null,

  @Column(name = "address_line_one")
  val addressLineOne: String? = null,

  @Column(name = "address_line_two")
  val addressLineTwo: String? = null,

  @Column(name = "address_line_three")
  val addressLineThree: String? = null,

  @Column(name = "address_line_four")
  val addressLineFour: String? = null,

  @Column(name = "address_line_five")
  val addressLineFive: String? = null,

  @Column(name = "postcode")
  val postcode: String? = null,

  @Column(name = "sex")
  val sex: String? = null,

  @Column(name = "nationality_one")
  val nationalityOne: String? = null,

  @Column(name = "nationality_two")
  val nationalityTwo: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

) : BaseAuditedEntity() {
  companion object {
    fun from(person: Person): DefendantEntity {
      return person.defendantId?.let {
        val defendantEntity = DefendantEntity(
          title = person.title,
          forenameOne = person.givenName,
          surname = person.familyName,
          dateOfBirth = person.dateOfBirth,
          defendantId = it,
          pncNumber = person.otherIdentifiers?.pncNumber,
          crn = person.otherIdentifiers?.crn,
          cro = person.otherIdentifiers?.cro,
          sex = person.sex,
          nationalityOne = person.nationalityOne,
          nationalityTwo = person.nationalityTwo,
          addressLineOne = person.addressLineOne,
          addressLineTwo = person.addressLineTwo,
          addressLineThree = person.addressLineThree,
          addressLineFour = person.addressLineFour,
          addressLineFive = person.addressLineFive,
          postcode = person.postcode,
        )
        defendantEntity.createdBy = PERSON_RECORD_SERVICE
        defendantEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
        return defendantEntity
      } ?: throw ValidationException("Missing defendant id")
    }
  }
}
