package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.CROIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.PNCIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDate

@Entity
@Table(name = "person")
class PersonEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "title")
  val title: String? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "middle_names")
  val middleNames: String? = null,

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  val aliases: MutableList<PersonAliasEntity> = mutableListOf(),

  @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "fk_address_id", referencedColumnName = "id", nullable = true)
  var address: AddressEntity? = null,

  @Column(name = "pnc")
  @Convert(converter = PNCIdentifierConverter::class)
  val pnc: PNCIdentifier? = null,

  @Column(name = "crn")
  val crn: String? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @Column(name = "most_recent_prisoner_number")
  val mostRecentPrisonerNumber: String? = null,

  @Column(name = "offender_id")
  val offenderId: String? = null,

  @Column(name = "defendant_id")
  val defendantId: String? = null,

  @Column(name = "master_defendant_id")
  val masterDefendantId: String? = null,

  @Column(name = "cro")
  @Convert(converter = CROIdentifierConverter::class)
  val cro: CROIdentifier? = null,

  @Column(name = "fingerprint")
  val fingerprint: Boolean = false,

  @Column(name = "national_insurance_number")
  val nationalInsuranceNumber: String? = null,

  @Column(name = "driver_license_number")
  val driverLicenseNumber: String? = null,

  @Column(name = "arrest_summons_number")
  val arrestSummonsNumber: String? = null,

  @Column(name = "telephone_number")
  val telephoneNumber: String? = null,

  @Column(name = "mobile_number")
  val mobileNumber: String? = null,

  @Column(name = "email_address")
  val emailAddress: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Column(name = "birth_place")
  val birthPlace: String? = null,

  @Column(name = "birth_country")
  val birthCountry: String? = null,

  @Column
  @Enumerated(EnumType.ORDINAL)
  val sourceSystem: SourceSystemType,

  @Version
  var version: Int = 0,

) {
  companion object {
    fun from(person: Person): PersonEntity {
      val personEntity = PersonEntity(
        title = person.title,
        firstName = person.givenName,
        middleNames = person.middleNames?.joinToString(" ") { it },
        lastName = person.familyName,
        dateOfBirth = person.dateOfBirth,
        birthPlace = person.birthPlace,
        birthCountry = person.birthCountry,
        defendantId = person.defendantId,
        pnc = person.otherIdentifiers?.pncIdentifier,
        crn = person.otherIdentifiers?.crn,
        cro = person.otherIdentifiers?.croIdentifier,
        fingerprint = person.otherIdentifiers?.croIdentifier?.fingerprint ?: false,
        prisonNumber = person.otherIdentifiers?.prisonNumber,
        mostRecentPrisonerNumber = person.otherIdentifiers?.mostRecentPrisonerNumber,
        driverLicenseNumber = person.driverNumber,
        arrestSummonsNumber = person.arrestSummonsNumber,
        masterDefendantId = person.masterDefendantId,
        nationalInsuranceNumber = person.nationalInsuranceNumber,
        emailAddress = person.primaryEmail,
        telephoneNumber = person.homePhone,
        mobileNumber = person.mobile,
        sourceSystem = person.sourceSystemType,
      )
      return personEntity
    }
  }
}
