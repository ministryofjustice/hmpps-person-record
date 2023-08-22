package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.*
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Entity
@Table(name = "delius_offender")
@Audited
class DeliusOffenderEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "crn")
  val crn: String,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,
) : BaseAuditedEntity(){
  companion object {
    fun from(person : Person) : DeliusOffenderEntity? {
      return person.otherIdentifiers?.crn?.let {
        DeliusOffenderEntity(
          crn = it
        )
      } ?: throw java.lang.IllegalArgumentException("Missing CRN")
    }
  }
}
