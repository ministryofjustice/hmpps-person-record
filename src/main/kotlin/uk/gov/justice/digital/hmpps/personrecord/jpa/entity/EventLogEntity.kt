package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.GeneratedColumn
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

  @Column(name = "first_name_aliases", columnDefinition = "text[]")
  val firstNameAliases: List<String> = emptyList(),

  @Column(name = "last_name_aliases", columnDefinition = "text[]")
  val lastNameAliases: List<String> = emptyList(),

  @Column(name = "date_of_birth_aliases", columnDefinition = "date[]")
  val dateOfBirthAliases: List<LocalDate> = emptyList(),

  @Column(columnDefinition = "text[]")
  val postcodes: List<String> = emptyList(),

  @Column(columnDefinition = "text[]")
  val cros: List<String> = emptyList(),

  @Column(columnDefinition = "text[]")
  val pncs: List<String> = emptyList(),

  @Column(name = "sentence_dates", columnDefinition = "date[]")
  val sentenceDates: List<LocalDate> = emptyList(),

  @Column(name = "override_marker")
  val overrideMarker: UUID? = null,

  @Column(name = "override_scopes", columnDefinition = "uuid[]")
  val overrideScopes: List<UUID> = emptyList(),

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
        personUUID = eventLog.clusterDetail?.personUUID ?: eventLog.personEntity.personKey?.personUUID,
        uuidStatusType = eventLog.clusterDetail?.status ?: eventLog.personEntity.personKey?.status,
        firstName = eventLog.personEntity.getPrimaryName().firstName,
        middleNames = eventLog.personEntity.getPrimaryName().middleNames,
        lastName = eventLog.personEntity.getPrimaryName().lastName,
        dateOfBirth = eventLog.personEntity.getPrimaryName().dateOfBirth,
        firstNameAliases = aliases.mapNotNull { it.firstName }.sorted().distinct(),
        lastNameAliases = aliases.mapNotNull { it.lastName }.sorted().distinct(),
        dateOfBirthAliases = aliases.mapNotNull { it.dateOfBirth }.sorted().distinct(),
        postcodes = eventLog.personEntity.addresses.mapNotNull { it.postcode }.sorted().distinct(),
        cros = eventLog.personEntity.references.getCROs().sorted().distinct(),
        pncs = eventLog.personEntity.references.getPNCs().sorted().distinct(),
        sentenceDates = eventLog.personEntity.sentenceInfo.mapNotNull { it.sentenceDate }.sorted().distinct(),
        overrideMarker = eventLog.personEntity.overrideMarker,
        overrideScopes = eventLog.personEntity.overrideScopes.map { it.scope }.sorted().distinct(),
        sourceSystem = eventLog.personEntity.sourceSystem,
        eventType = eventLog.eventType,
        recordMergedTo = eventLog.personEntity.mergedTo,
        statusReason = eventLog.personEntity.personKey?.statusReason,
        eventTimestamp = eventLog.eventTimestamp,
      )
    }
  }
}
