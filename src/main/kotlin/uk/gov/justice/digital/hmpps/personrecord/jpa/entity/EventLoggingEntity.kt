package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "event_logging")
class EventLoggingEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "operation_id")
  var operationId: String? = null,

  @Column(name = "before_data")
  var beforeData: String? = null,

  @Column(name = "processed_data")
  var processedData: String? = null,

  @Column(name = "source_system_id")
  var sourceSystemId: String? = null,

  @Column(name = "uuid")
  var uuid: String? = null,

  @Column(name = "source_system")
  var sourceSystem: String? = null,

  @Column(name = "message_event_type")
  var messageEventType: String? = null,

  @Column(name = "event_timestamp")
  val eventTimestamp: LocalDateTime? = null,

)
