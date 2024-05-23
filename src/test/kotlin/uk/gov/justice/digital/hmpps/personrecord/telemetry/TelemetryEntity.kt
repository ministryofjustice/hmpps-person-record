package uk.gov.justice.digital.hmpps.personrecord.telemetry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "telemetry")
data class TelemetryEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @Column
  var event: String? = null,

  @Column(columnDefinition = "json")
  var properties: String? = null,

  @Version
  var version: Int = 0,

)
