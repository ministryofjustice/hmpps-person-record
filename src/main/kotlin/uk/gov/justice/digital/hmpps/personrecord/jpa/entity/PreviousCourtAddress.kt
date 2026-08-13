package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "previous_court_addresss")
class PreviousCourtAddress(
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

  @Column
  var postcode: String? = null,

  @Column(name = "building_name")
  var buildingName: String? = null,

  @Column(name = "building_number")
  var buildingNumber: String? = null,

  @Column(name = "thoroughfare_name")
  var thoroughfareName: String? = null,

  @Column(name = "dependent_locality")
  var dependentLocality: String? = null,

  @Column(name = "post_town")
  var postTown: String? = null,

  @Version
  var version: Int = 0,
)
