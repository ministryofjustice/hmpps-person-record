package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.PersonAlias
import java.time.LocalDate

@Entity
@Table(name = "person_alias")
class PersonAliasEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_names")
  val middleNames: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Version
  var version: Int = 0,
) {
  companion object {
    private fun from(personAlias: PersonAlias): PersonAliasEntity? =
      when {
        isAliasPresent(personAlias.firstName, personAlias.middleNames, personAlias.lastName) ->
          PersonAliasEntity(
            firstName = personAlias.firstName,
            middleNames = personAlias.middleNames,
            lastName = personAlias.lastName,
          )
        else -> null
      }

    fun fromList(personAliases: List<PersonAlias>): List<PersonAliasEntity> = personAliases.mapNotNull { from(it) }

    private fun isAliasPresent(firstName: String?, middleName: String?, surname: String?): Boolean =
      sequenceOf(firstName, middleName, surname)
        .filterNotNull().any { it.isNotBlank() }
  }
}
