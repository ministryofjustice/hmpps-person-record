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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.extractSourceSystemId
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
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
  val sourceSystemId: String? = null,

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

  @Type(StringArrayType::class)
  @Column(name = "override_markers", columnDefinition = "text[]")
  val overrideMarkers: Array<String> = emptyArray<String>(),

  @Enumerated(STRING)
  @Column(name = "source_system")
  val sourceSystem: SourceSystemType,

  @Column(name = "event_type")
  val eventType: String? = null,

  @Column(name = "operation_id")
  val operationId: String? = null,

  @Column(name = "record_merged_to")
  val recordMergedTo: Long? = null,

  @Column(name = "cluster_composition")
  val clusterComposition: String? = null,

  @Column(
    name = "event_timestamp",
    insertable = false,
    updatable = false,
  )
  val eventTimestamp: LocalDateTime? = null,

  ) {
  companion object {

    fun from(personEntity: PersonEntity): EventLogEntity {
      val personKeyEntity = personEntity.personKey!!
      return EventLogEntity(
        sourceSystemId = personEntity.extractSourceSystemId(),
        matchId = personEntity.matchId,
        uuid = personKeyEntity.personId!!,
        uuidStatusType = personKeyEntity.status,
        firstName = personEntity.firstName,
        middleNames = personEntity.middleNames,
        lastName = personEntity.lastName,
        dateOfBirth = personEntity.dateOfBirth,
        firstNameAliases = personEntity.pseudonyms.mapNotNull { it.firstName }.sorted().toTypedArray(),
        lastNameAliases = personEntity.pseudonyms.mapNotNull { it.lastName }.sorted().toTypedArray(),
        dateOfBirthAliases = personEntity.pseudonyms.mapNotNull { it.dateOfBirth }.sorted().toTypedArray(),
        postcodes = personEntity.addresses.mapNotNull { it.postcode }.sorted().toTypedArray(),
        cros = personEntity.references.getType(IdentifierType.CRO).mapNotNull { it.identifierValue }.sorted().toTypedArray(),
        pncs = personEntity.references.getType(IdentifierType.PNC).mapNotNull { it.identifierValue }.sorted().toTypedArray(),
        sentenceDates = personEntity.sentenceInfo.mapNotNull { it.sentenceDate }.sorted().toTypedArray(),
        overrideMarkers = emptyArray(),
        sourceSystem = personEntity.sourceSystem,
        eventType = null,
        operationId = null,
        recordMergedTo = personEntity.mergedTo,
        clusterComposition = null,
      )
    }
  }
}
