package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import io.hypersistence.utils.hibernate.type.array.LocalDateArrayType
import io.hypersistence.utils.hibernate.type.array.LongArrayType
import io.hypersistence.utils.hibernate.type.array.StringArrayType
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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.extractSourceSystemId
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
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

  @Type(LongArrayType::class)
  @Column(name = "exclude_override_markers", columnDefinition = "bigint[]")
  val excludeOverrideMarkers: Array<Long> = emptyArray<Long>(),

  @Type(LongArrayType::class)
  @Column(name = "include_override_markers", columnDefinition = "bigint[]")
  val includeOverrideMarkers: Array<Long> = emptyArray<Long>(),

  @Enumerated(STRING)
  @Column(name = "source_system")
  val sourceSystem: SourceSystemType? = null,

  @Enumerated(STRING)
  @Column(name = "event_type")
  val eventType: CPRLogEvents,

  @Column(name = "record_merged_to")
  val recordMergedTo: Long? = null,

  @Column(name = "cluster_composition")
  val clusterComposition: String? = null,

  @GeneratedColumn("event_timestamp")
  val eventTimestamp: LocalDateTime? = null,

) {
  companion object {

    fun from(
      personEntity: PersonEntity,
      eventType: CPRLogEvents,
      clusterComposition: String? = null,
      personKeyEntity: PersonKeyEntity? = null,
    ): EventLogEntity {
      val aliases: List<PseudonymEntity> = personEntity.getAliases()
      return EventLogEntity(
        sourceSystemId = personEntity.extractSourceSystemId(),
        matchId = personEntity.matchId,
        personUUID = personKeyEntity?.personUUID ?: personEntity.personKey?.personUUID,
        uuidStatusType = personKeyEntity?.status ?: personEntity.personKey?.status,
        firstName = personEntity.getPrimaryName().firstName,
        middleNames = personEntity.getPrimaryName().middleNames,
        lastName = personEntity.getPrimaryName().lastName,
        dateOfBirth = personEntity.getPrimaryName().dateOfBirth,
        firstNameAliases = aliases.mapNotNull { it.firstName }.dedupeAndSortedArray(),
        lastNameAliases = aliases.mapNotNull { it.lastName }.dedupeAndSortedArray(),
        dateOfBirthAliases = aliases.mapNotNull { it.dateOfBirth }.dedupeAndSortedArray(),
        postcodes = personEntity.addresses.mapNotNull { it.postcode }.dedupeAndSortedArray(),
        cros = personEntity.references.getType(IdentifierType.CRO).mapNotNull { it.identifierValue }.dedupeAndSortedArray(),
        pncs = personEntity.references.getType(IdentifierType.PNC).mapNotNull { it.identifierValue }.dedupeAndSortedArray(),
        sentenceDates = personEntity.sentenceInfo.mapNotNull { it.sentenceDate }.dedupeAndSortedArray(),
        excludeOverrideMarkers = personEntity.getExcludeOverrideMarkers().mapNotNull { it.markerValue }.dedupeAndSortedArray(),
        includeOverrideMarkers = personEntity.getIncludeOverrideMarkers().mapNotNull { it.markerValue }.dedupeAndSortedArray(),
        sourceSystem = personEntity.sourceSystem,
        eventType = eventType,
        recordMergedTo = personEntity.mergedTo,
        clusterComposition = clusterComposition,
      )
    }

    private fun List<String>.dedupeAndSortedArray() = this.sorted().distinct().toTypedArray()

    private fun List<Long>.dedupeAndSortedArray() = this.sorted().distinct().toTypedArray()

    private fun List<LocalDate>.dedupeAndSortedArray() = this.sorted().distinct().toTypedArray()
  }
}
