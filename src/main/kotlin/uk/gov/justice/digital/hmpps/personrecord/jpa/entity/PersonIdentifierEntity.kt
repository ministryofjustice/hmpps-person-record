package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.*

@Entity
@Table(name = "person_identifier")
class PersonIdentifierEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "person_id")
  val personId: UUID? = null,

  @Column
  @OneToMany(mappedBy = "personIdentifier", cascade = [CascadeType.ALL], orphanRemoval = true)
  var personEntities: MutableList<PersonEntity> = mutableListOf(),

  @Version
  var version: Int = 0,

) {
  companion object {
    fun new(): PersonIdentifierEntity = PersonIdentifierEntity(personId = UUID.randomUUID())
  }
}
