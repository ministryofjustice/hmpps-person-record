package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.*
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.util.*

private const val PERSON_RECORD_SERVICE = "PERSON-RECORD-SERVICE"

@Entity
@Table(name = "person")
@Audited
class  PersonEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "person_id")
  val personId: UUID? = null,

  @OneToMany(mappedBy = "person",  cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var deliusOffenders: MutableList<DeliusOffenderEntity> = mutableListOf(),

  @OneToMany(mappedBy = "person",  cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var hmctsDefendants: MutableList<HmctsDefendantEntity> = mutableListOf(),

  ) : BaseAuditedEntity() {
  companion object {
    fun from(person: Person): PersonEntity {
      person.dateOfBirth
      val personEntity = PersonEntity(
        personId = UUID.randomUUID(),
      )

      personEntity.createdBy = PERSON_RECORD_SERVICE
      personEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
      return personEntity
    }
  }
}
