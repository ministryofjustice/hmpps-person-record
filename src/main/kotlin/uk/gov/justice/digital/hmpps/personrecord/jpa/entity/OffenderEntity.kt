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
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.CROIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.PNCIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import java.time.LocalDate

@Entity
@Table(name = "offender")
class OffenderEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "crn")
  val crn: String,

  @Column(name = "pnc_number")
  @Convert(converter = PNCIdentifierConverter::class)
  val pncNumber: PNCIdentifier? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_name")
  val middleName: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Column(name = "lao")
  val isLimitedAccessOffender: Boolean? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @Column(name = "gender")
  val gender: String? = null,

  @Column(name = "title")
  val title: String? = null,

  @Column(name = "cro")
  @Convert(converter = CROIdentifierConverter::class)
  val cro: CROIdentifier? = null,

  @Column(name = "ni_number")
  val nationalInsuranceNumber: String? = null,

  @Column(name = "offender_id")
  val offenderId: Long? = null,

  @Column(name = "most_recent_prison_number")
  val mostRecentPrisonNumber: String? = null,

  @Column(name = "preferred_name")
  val preferredName: String? = null,

  @Column(name = "previous_surname")
  val previousSurname: String? = null,

  @Column(name = "ethnicity")
  val ethnicity: String? = null,

  @Column(name = "nationality")
  val nationality: String? = null,

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

  @OneToMany(mappedBy = "offender", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var aliases: MutableList<OffenderAliasEntity> = mutableListOf(),

  @Version
  var version: Int = 0,

) {
  companion object {
    fun from(person: Person): OffenderEntity {
      return person.otherIdentifiers?.crn?.let {
        val offenderEntity = OffenderEntity(
          crn = it,
          cro = person.otherIdentifiers.croIdentifier,
          pncNumber = person.otherIdentifiers.pncIdentifier,
          firstName = person.givenName,
          lastName = person.familyName,
          dateOfBirth = person.dateOfBirth,
          prisonNumber = person.otherIdentifiers.prisonNumber,
        )
        return offenderEntity
      } ?: throw java.lang.IllegalArgumentException("Missing CRN")
    }

    fun from(offenderDetail: OffenderDetail): OffenderEntity {
      val offenderEntity = OffenderEntity(
        crn = offenderDetail.otherIds.crn,
        cro = CROIdentifier.from(offenderDetail.otherIds.croNumber),
        pncNumber = PNCIdentifier.from(offenderDetail.otherIds.pncNumber),
        nationalInsuranceNumber = offenderDetail.otherIds.niNumber,
        prisonNumber = offenderDetail.otherIds.nomsNumber,
        offenderId = offenderDetail.offenderId,
        mostRecentPrisonNumber = offenderDetail.otherIds.mostRecentPrisonerNumber,
        title = offenderDetail.title,
        firstName = offenderDetail.firstName,
        middleName = offenderDetail.middleNames?.joinToString(" ") { it },
        lastName = offenderDetail.surname,
        dateOfBirth = offenderDetail.dateOfBirth,
        previousSurname = offenderDetail.previousSurName,
        preferredName = offenderDetail.highlight?.getPreferredName(),
        contact = offenderDetail.contactDetails?.let { ContactEntity.from(it) },
        address = offenderDetail.contactDetails?.getAddress()?.let { AddressEntity.from(it) },
      )
      return offenderEntity
    }
  }
}
