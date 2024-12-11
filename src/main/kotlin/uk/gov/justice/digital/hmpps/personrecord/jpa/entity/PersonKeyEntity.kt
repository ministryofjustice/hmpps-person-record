package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import java.util.*

@Entity
@Table(name = "personkey")
class PersonKeyEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  val id: Long? = null,

  @Column(name = "person_id")
  val personId: UUID? = null,

  @Column
  @OneToMany(mappedBy = "personKey", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  @JsonManagedReference
  var personEntities: MutableList<PersonEntity> = mutableListOf(),

  @Column(name = "merged_to")
  var mergedTo: Long? = null,

  @Column
  @Enumerated(STRING)
  var status: UUIDStatusType = UUIDStatusType.ACTIVE,

  @Version
  @JsonIgnore
  var version: Int = 0,

) {

  companion object {
    val empty: PersonKeyEntity? = null

    fun new(): PersonKeyEntity = PersonKeyEntity(personId = UUID.randomUUID())
  }
}
