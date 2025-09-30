package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import io.hypersistence.utils.hibernate.type.array.LocalDateArrayType
import io.hypersistence.utils.hibernate.type.array.StringArrayType
import io.hypersistence.utils.hibernate.type.array.UUIDArrayType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.GeneratedColumn
import org.hibernate.annotations.Type
import uk.gov.justice.digital.hmpps.personrecord.extensions.getCROs
import uk.gov.justice.digital.hmpps.personrecord.extensions.getPNCs
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
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
  val sourceSystemId: String? = null,

  @Column(name = "master_defendant_id")
  val masterDefendantId: String? = null,

  @Column(name = "match_id")
  val matchId: UUID? = null,

  @Column(name = "person_uuid")
  val personUUID: UUID? = null,

  @Column(name = "uuid_status_type")
  @Enumerated(STRING)
  val uuidStatusType: UUIDStatusType? = null,

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
  val firstNameAliases: Array<String> = emptyArray<String>(),

  @Type(StringArrayType::class)
  @Column(name = "last_name_aliases", columnDefinition = "text[]")
  val lastNameAliases: Array<String> = emptyArray<String>(),

  @Type(LocalDateArrayType::class)
  @Column(name = "date_of_birth_aliases", columnDefinition = "date[]")
  val dateOfBirthAliases: Array<LocalDate> = emptyArray<LocalDate>(),

  @Type(StringArrayType::class)
  @Column(columnDefinition = "text[]")
  val postcodes: Array<String> = emptyArray<String>(),

  @Type(StringArrayType::class)
  @Column(columnDefinition = "text[]")
  val cros: Array<String> = emptyArray<String>(),

  @Type(StringArrayType::class)
  @Column(columnDefinition = "text[]")
  val pncs: Array<String> = emptyArray<String>(),

  @Type(LocalDateArrayType::class)
  @Column(name = "sentence_dates", columnDefinition = "date[]")
  val sentenceDates: Array<LocalDate> = emptyArray<LocalDate>(),

  @Column(name = "override_marker")
  val overrideMarker: UUID? = null,

  @Type(UUIDArrayType::class)
  @Column(name = "override_scopes", columnDefinition = "uuid[]")
  val overrideScopes: Array<UUID> = emptyArray<UUID>(),

  @Enumerated(STRING)
  @Column(name = "source_system")
  val sourceSystem: SourceSystemType? = null,

  @Enumerated(STRING)
  @Column(name = "event_type")
  val eventType: CPRLogEvents,

  @Column(name = "record_merged_to")
  val recordMergedTo: Long? = null,

  @GeneratedColumn("event_timestamp")
  val eventTimestamp: LocalDateTime? = null,

  @Enumerated(STRING)
  @Column(name = "status_reason")
  val statusReason: UUIDStatusReasonType? = null,

) {
  companion object {

    fun from(eventLog: RecordEventLog): EventLogEntity {
      val aliases: List<PseudonymEntity> = eventLog.personEntity.getAliases()
      return EventLogEntity(
        sourceSystemId = eventLog.personEntity.extractSourceSystemId(),
        masterDefendantId = eventLog.personEntity.masterDefendantId,
        matchId = eventLog.personEntity.matchId,
        personUUID = eventLog.personKeyEntity?.personUUID ?: eventLog.personEntity.personKey?.personUUID,
        uuidStatusType = eventLog.personKeyEntity?.status ?: eventLog.personEntity.personKey?.status,
        firstName = eventLog.personEntity.getPrimaryName().firstName,
        middleNames = eventLog.personEntity.getPrimaryName().middleNames,
        lastName = eventLog.personEntity.getPrimaryName().lastName,
        dateOfBirth = eventLog.personEntity.getPrimaryName().dateOfBirth,
        firstNameAliases = aliases.mapNotNull { it.firstName }.dedupeAndSortedArray(),
        lastNameAliases = aliases.mapNotNull { it.lastName }.dedupeAndSortedArray(),
        dateOfBirthAliases = aliases.mapNotNull { it.dateOfBirth }.dedupeAndSortedArray(),
        postcodes = eventLog.personEntity.addresses.mapNotNull { it.postcode }.dedupeAndSortedArray(),
        cros = eventLog.personEntity.references.getCROs().dedupeAndSortedArray(),
        pncs = eventLog.personEntity.references.getPNCs().dedupeAndSortedArray(),
        sentenceDates = eventLog.personEntity.sentenceInfo.mapNotNull { it.sentenceDate }.dedupeAndSortedArray(),
        overrideMarker = eventLog.personEntity.overrideMarker,
        overrideScopes = eventLog.personEntity.overrideScopes.map { it.scope }.dedupeAndSortedArray(),
        sourceSystem = eventLog.personEntity.sourceSystem,
        eventType = eventLog.eventType,
        recordMergedTo = eventLog.personEntity.mergedTo,
        statusReason = eventLog.personEntity.personKey?.statusReason,
        eventTimestamp = eventLog.eventTimestamp,
      )
    }

    private fun List<String>.dedupeAndSortedArray() = this.sorted().distinct().toTypedArray()

    private fun List<UUID>.dedupeAndSortedArray() = this.sorted().distinct().toTypedArray()

    private fun List<LocalDate>.dedupeAndSortedArray() = this.sorted().distinct().toTypedArray()
  }
}
