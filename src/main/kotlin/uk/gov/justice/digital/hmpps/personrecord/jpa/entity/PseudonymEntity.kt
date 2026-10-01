package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "pseudonym")
class PseudonymEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(
    name = "update_id",
    insertable = false,
    updatable = false,
    nullable = false,
  )
  @Generated
  var updateId: UUID? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @OneToMany(mappedBy = "pseudonym", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var references: MutableList<ReferenceEntity> = mutableListOf(),

  @Column(name = "title_code")
  @Enumerated(STRING)
  var titleCode: TitleCode? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_names")
  val middleNames: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Column(name = "sex_code")
  @Enumerated(STRING)
  var sexCode: SexCode? = null,

  @Column(name = "ethnicity_code")
  @Enumerated(STRING)
  var ethnicityCode: EthnicityCode? = null,

  @Column(name = "name_type")
  @Enumerated(STRING)
  val nameType: NameType,

  @Version
  var version: Int = 0,
) {

  fun update(alias: Alias) {
    updateReferences(alias.references.map { ReferenceEntity.from(it) }.toMutableList())
  }

  fun updateReferences(references: MutableList<ReferenceEntity>) {
    this.references.clear()
    references.forEach { reference ->
      reference.pseudonym = this
    }
    this.references.addAll(references)
  }

  companion object {
    fun primaryNameFrom(person: Person): PseudonymEntity = PseudonymEntity(
      firstName = person.firstName,
      middleNames = person.middleNames,
      lastName = person.lastName,
      nameType = NameType.PRIMARY,
      titleCode = person.titleCode,
      dateOfBirth = person.dateOfBirth,
      sexCode = person.sexCode,
      ethnicityCode = person.ethnicityCode,
    )

    fun aliasFrom(alias: Alias): PseudonymEntity? = when {
      isAliasPresent(alias.firstName, alias.middleNames, alias.lastName) ->
        PseudonymEntity(
          firstName = alias.firstName,
          middleNames = alias.middleNames,
          lastName = alias.lastName,
          dateOfBirth = alias.dateOfBirth,
          nameType = NameType.ALIAS,
          titleCode = alias.titleCode,
          sexCode = alias.sexCode,
          references = alias.references.map { ReferenceEntity.from(it) }.toMutableList(),
        ).also { it.update(alias) }
      else -> null
    }

    private fun isAliasPresent(firstName: String?, middleNames: String?, surname: String?): Boolean = sequenceOf(firstName, middleNames, surname)
      .filterNotNull().any { it.isNotBlank() }
  }
}
