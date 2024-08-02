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

@Entity
@Table(name = "court_hearing")
class CourtHearingEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "hearing_id")
  val hearingId: String? = null,

  @Column
  @OneToMany(mappedBy = "courtHearing", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var messages: MutableList<CourtMessageEntity> = mutableListOf(),

  @Version
  var version: Int = 0,
)
