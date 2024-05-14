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
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import java.time.LocalDate

@Entity
@Table(name = "alias")
class AliasEntity(

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
    private fun from(alias: Alias): AliasEntity? =
      when {
        isAliasPresent(alias.firstName, alias.middleNames, alias.lastName) ->
          AliasEntity(
            firstName = alias.firstName,
            middleNames = alias.middleNames,
            lastName = alias.lastName,
          )
        else -> null
      }

    fun fromList(aliases: List<Alias>): List<AliasEntity> = aliases.mapNotNull { from(it) }

    private fun isAliasPresent(firstName: String?, middleNames: String?, surname: String?): Boolean =
      sequenceOf(firstName, middleNames, surname)
        .filterNotNull().any { it.isNotBlank() }
  }
}
