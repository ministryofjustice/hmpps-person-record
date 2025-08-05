package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "nationality_codes")
class NationalityCodeEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(unique = true, nullable = false)
  val code: String,

  @Column
  val description: String,
)
