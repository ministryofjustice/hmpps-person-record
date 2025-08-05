package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.TitleCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import java.time.LocalDate

@Entity
@Table(name = "pseudonym")
class PseudonymEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @ManyToOne
  @JoinColumn(
    name = "fk_title_code_id",
    referencedColumnName = "id",
  )
  var titleCode: TitleCodeEntity? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_names")
  val middleNames: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Column(name = "name_type")
  @Enumerated(STRING)
  val nameType: NameType,

  @Version
  var version: Int = 0,
) {
  companion object {
    fun primaryNameFrom(person: Person, titleCode: TitleCodeEntity?): PseudonymEntity = PseudonymEntity(
      firstName = person.firstName,
      middleNames = person.middleNames,
      lastName = person.lastName,
      nameType = NameType.PRIMARY,
      titleCode = titleCode,
      dateOfBirth = person.dateOfBirth,
    )

    fun aliasFrom(alias: Alias, titleCode: TitleCodeEntity?): PseudonymEntity? = when {
      isAliasPresent(alias.firstName, alias.middleNames, alias.lastName) ->
        PseudonymEntity(
          firstName = alias.firstName,
          middleNames = alias.middleNames,
          lastName = alias.lastName,
          dateOfBirth = alias.dateOfBirth,
          nameType = NameType.ALIAS,
          titleCode = titleCode,
        )
      else -> null
    }

    private fun isAliasPresent(firstName: String?, middleNames: String?, surname: String?): Boolean = sequenceOf(firstName, middleNames, surname)
      .filterNotNull().any { it.isNotBlank() }
  }
}
