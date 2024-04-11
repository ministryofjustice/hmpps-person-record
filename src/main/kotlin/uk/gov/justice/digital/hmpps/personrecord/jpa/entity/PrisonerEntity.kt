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
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.PrisonerDetails
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.CROIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.PNCIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import java.time.LocalDate

@Entity
@Table(name = "prisoner")
class PrisonerEntity(

        @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

        @Column(name = "prison_number")
  val prisonNumber: String? = null,

        @Column(name = "pnc_number")
  @Convert(converter = PNCIdentifierConverter::class)
  val pncNumber: PNCIdentifier? = null,

        @Column(name = "cro")
        @Convert(converter = CROIdentifierConverter::class)
  val cro: String? = null,

        @Column(name = "offender_id")
  val offenderId: Long? = null,

        @Column(name = "root_offender_id")
  val rootOffenderId: Long? = null,

        @Column(name = "ni_number")
  val nationalInsuranceNumber: String? = null,

        @Column(name = "driving_license_number")
  val drivingLicenseNumber: String? = null,

        @Column(name = "birth_place")
  val birthPlace: String? = null,

        @Column(name = "birth_country_code")
  val birthCountryCode: String? = null,

        @Column(name = "sex_code")
  val sexCode: String? = null,

        @Column(name = "race_code")
  val raceCode: String? = null,

        @Column(name = "title")
  val title: String? = null,

        @Column(name = "first_name")
  val firstName: String? = null,

        @Column(name = "middle_name")
  val middleName: String? = null,

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

        @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "fk_address_id", referencedColumnName = "id", nullable = true)
  var address: AddressEntity? = null,

        @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "fk_contact_id", referencedColumnName = "id", nullable = true)
  var contact: ContactEntity? = null,

        @OneToMany(mappedBy = "prisoner", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var aliases: MutableList<PrisonerAliasEntity> = mutableListOf(),

        @Version
  var version: Int = 0,

        ) {
  companion object {
    fun from(prisoner: Prisoner): PrisonerEntity {
      return PrisonerEntity(
        prisonNumber = prisoner.prisonerNumber,
        pncNumber = PNCIdentifier.from(prisoner.pncNumber),
        firstName = prisoner.firstName,
        lastName = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
      )
    }
    fun from(prisonerDetails: PrisonerDetails): PrisonerEntity {
      return PrisonerEntity(
        prisonNumber = prisonerDetails.offenderNo,
        pncNumber = PNCIdentifier.from(prisonerDetails.getPnc()),
        cro = prisonerDetails.getCro(),
        offenderId = prisonerDetails.offenderId,
        rootOffenderId = prisonerDetails.rootOffenderId,
        drivingLicenseNumber = prisonerDetails.getDrivingLicenseNumber(),
        nationalInsuranceNumber = prisonerDetails.getNationalInsuranceNumber(),
        title = prisonerDetails.title,
        firstName = prisonerDetails.firstName,
        middleName = prisonerDetails.middleName,
        lastName = prisonerDetails.lastName,
        dateOfBirth = prisonerDetails.dateOfBirth,
        contact = ContactEntity.from(prisonerDetails),
        address = when {
          prisonerDetails.getHomeAddress()?.postCode?.isNotBlank() == true -> {
            AddressEntity(postcode = prisonerDetails.getHomeAddress()?.postCode)
          }
          else -> {
            null
          }
        },
      )
    }
  }
}
