package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.*
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.time.LocalDate
import java.util.*

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
  val dateOfBirth: LocalDate? = null,

) : BaseAuditedEntity() {
  companion object {
    fun from(person: Person): PersonEntity {
      val personEntity = PersonEntity(
        givenName = person.givenName,
        familyName = person.familyName,
        middleNames = person.middleNames?.joinToString(separator = " "),
        pncNumber = person.otherIdentifiers?.pncNumber,
        crn = person.otherIdentifiers?.crn,
        dateOfBirth = person.dateOfBirth,
        personId = UUID.randomUUID(),
      )

      personEntity.createdBy = PERSON_RECORD_SERVICE
      personEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
      return personEntity
    }
  }
}
