package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.*

@Entity
@Table(name = "personkey")
class PersonKeyEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "person_id")
  val personId: UUID? = null,

  @Column
  @OneToMany(mappedBy = "personKey", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  var personEntities: MutableList<PersonEntity> = mutableListOf(),

  @Version
  var version: Int = 0,

) {
  companion object {
    val none: PersonKeyEntity? = null

    fun new(): PersonKeyEntity = PersonKeyEntity(personId = UUID.randomUUID())
  }
}
