package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.PNCIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.Person
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

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Column(name = "lao")
  val isLimitedAccessOffender: Boolean? = null,

  @Column(name = "prison_number")
  val prisonNumber: String? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Version
  var version: Int = 0,

) {
  companion object {
    fun from(person: Person): OffenderEntity {
      return person.otherIdentifiers?.crn?.let {
        val offenderEntity = OffenderEntity(
          crn = it,
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
      )
      return offenderEntity
    }
  }
}
