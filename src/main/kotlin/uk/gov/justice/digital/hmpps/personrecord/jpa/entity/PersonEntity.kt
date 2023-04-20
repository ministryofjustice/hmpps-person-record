package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.model.PersonDTO
import java.time.LocalDate
import java.util.UUID

private const val PERSON_RECORD_SERVICE = "PERSON-RECORD-SERVICE"

@Entity
@Table(name = "person")
@Audited
data class PersonEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "person_id")
  val personId: UUID? = null,

  @Column(name = "pnc_number")
  val pncNumber: String? = null,

  @Column(name = "crn")
  val crn: String? = null,

  @Column(name = "given_name")
  val givenName: String? = null,

  @Column(name = "family_name")
  val familyName: String,

  @Column(name = "middle_names")
  val middleNames: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate,

) : BaseAuditedEntity() {
  companion object {
    fun from(personDTO: PersonDTO): PersonEntity {
      var personEntity = PersonEntity(
        givenName = personDTO.givenName,
        familyName = personDTO.familyName,
        middleNames = personDTO.middleNames?.joinToString(separator = " "),
        pncNumber = personDTO?.otherIdentifiers?.pncNumber,
        crn = personDTO?.otherIdentifiers?.crn,
        dateOfBirth = personDTO.dateOfBirth,
        personId = UUID.randomUUID(),
      )

      personEntity.createdBy = PERSON_RECORD_SERVICE
      personEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
      return personEntity
    }
  }
}
