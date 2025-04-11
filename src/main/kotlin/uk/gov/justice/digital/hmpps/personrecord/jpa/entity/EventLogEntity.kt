package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import io.hypersistence.utils.hibernate.type.array.LocalDateArrayType
import io.hypersistence.utils.hibernate.type.array.StringArrayType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "event_log")
class EventLogEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "source_system_id")
  val sourceSystemId: String,

  @Column(name = "match_id")
  val matchId: UUID,

  val uuid: UUID,

  @Column(name = "uuid_status_type")
  @Enumerated(STRING)
  val uuidStatusType: UUIDStatusType,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_names")
  val middleNames: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Type(StringArrayType::class)
  @Column(name = "first_name_aliases", columnDefinition = "text[]")
  val firstNameAliases: List<String> = emptyList(),

  @Type(StringArrayType::class)
  @Column(name = "last_name_aliases", columnDefinition = "text[]")
  val lastNameAliases: List<String> = emptyList(),

  @Type(LocalDateArrayType::class)
  @Column(name = "date_of_birth_aliases", columnDefinition = "date[]")
  val dateOfBirthAliases: List<LocalDate> = emptyList(),

  @Type(StringArrayType::class)
  @Column(columnDefinition = "text[]")
  val postcodes: List<String> = emptyList(),

  @Type(StringArrayType::class)
  @Column(columnDefinition = "text[]")
  val cros: List<String> = emptyList(),

  @Type(StringArrayType::class)
  @Column(columnDefinition = "text[]")
  val pncs: List<String> = emptyList(),

  @Type(LocalDateArrayType::class)
  @Column(name = "sentence_dates", columnDefinition = "date[]")
  val sentenceDates: List<LocalDate> = emptyList(),

  @Type(StringArrayType::class)
  @Column(name = "override_markers", columnDefinition = "text[]")
  val overrideMarkers: List<String> = emptyList(),

  @Enumerated(STRING)
  @Column(name = "source_system")
  val sourceSystem: SourceSystemType,

  @Column(name = "event_type")
  val eventType: String? = null,

  @Column(name = "operation_id")
  val operationId: String? = null,

  @Column(name = "record_merged_to")
  val recordMergedTo: String? = null,

  @Column(name = "cluster_composition")
  val clusterComposition: String? = null,

  @Column(name = "event_timestamp")
  val eventTimestamp: LocalDateTime? = null,
)
