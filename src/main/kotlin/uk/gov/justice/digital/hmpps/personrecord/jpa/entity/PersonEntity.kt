package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "person")
@Audited
data class PersonEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long,

  @Column(name = "person_id")
  val personId: UUID? = null,

  @Column(name = "pnc_number")
  val pncNumber: String? = null,

  @Column(name = "crn")
  val crn: String? = null,

  @Column(name = "given_name")
  val givenName: String? = null,

  @Column(name = "family_name")
  val familyName: String? = null,

  @Column(name = "middle_names")
  val middleNames: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate,

) : BaseAuditedEntity()
