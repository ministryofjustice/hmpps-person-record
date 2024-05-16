package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.person.name.Name
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import java.time.LocalDate

@Entity
@Table(name = "name")
class NameEntity(

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

  @Column
  var title: String? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_names")
  val middleNames: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Column
  @Enumerated(STRING)
  val type: NameType,

  @Version
  var version: Int = 0,
) {
  companion object {
    private fun from(name: Name): NameEntity? =
      when {
        isAliasPresent(name.title, name.firstName, name.middleNames, name.lastName) ->
          NameEntity(
            title = name.title,
            firstName = name.firstName,
            middleNames = name.middleNames,
            lastName = name.lastName,
            dateOfBirth = name.dateOfBirth,
            type = name.type,
          )
        else -> null
      }

    fun fromList(names: List<Name>): List<NameEntity> = names.mapNotNull { from(it) }

    private fun isAliasPresent(title: String?, firstName: String?, middleNames: String?, surname: String?): Boolean =
      sequenceOf(title, firstName, middleNames, surname)
        .filterNotNull().any { it.isNotBlank() }
  }
}
