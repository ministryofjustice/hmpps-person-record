package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.PNCIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.time.LocalDate

@Entity
@Table(name = "defendant")
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

  @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "fk_address_id", referencedColumnName = "id", nullable = true)
  var address: AddressEntity? = null,

  @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "fk_contact_id", referencedColumnName = "id", nullable = true)
  var contact: ContactEntity? = null,

  @OneToMany(mappedBy = "defendant", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var aliases: MutableList<DefendantAliasEntity> = mutableListOf(),

  @Column(name = "defendant_id")
  val defendantId: String? = null,

  @Column(name = "pnc_number")
  @Convert(converter = PNCIdentifierConverter::class)
  val pncNumber: PNCIdentifier? = null,

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

  @Column(name = "sex")
  val sex: String? = null,

  @Column(name = "nationality_one")
  val nationalityOne: String? = null,

  @Column(name = "nationality_two")
  val nationalityTwo: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Version
  var version: Int = 0,

) {
  companion object {
    fun from(person: Person): DefendantEntity {
      val defendantEntity = DefendantEntity(
        title = person.title,
        forenameOne = person.givenName,
        surname = person.familyName,
        dateOfBirth = person.dateOfBirth,
        defendantId = person.defendantId,
        pncNumber = person.otherIdentifiers?.pncIdentifier,
        crn = person.otherIdentifiers?.crn,
        cro = person.otherIdentifiers?.cro,
        sex = person.sex,
        nationalityOne = person.nationalityOne,
        nationalityTwo = person.nationalityTwo,
        address = AddressEntity.from(person),
        contact = ContactEntity.from(person),

      )
      return defendantEntity
    }
  }
}
