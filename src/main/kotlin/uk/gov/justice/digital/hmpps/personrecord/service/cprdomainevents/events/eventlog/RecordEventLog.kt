package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog

import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.ValidCluster
import uk.gov.justice.digital.hmpps.personrecord.extensions.getCROs
import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RecordEventLog(
  val sourceSystemId: String? = null,
  val matchId: UUID? = null,
  val personUUID: UUID? = null,
  val uuidStatusType: UUIDStatusType? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val firstNameAliases: List<String> = emptyList(),
  val lastNameAliases: List<String> = emptyList(),
  val dateOfBirthAliases: List<LocalDate> = emptyList(),
  val postcodes: List<String> = emptyList(),
  val cros: List<String> = emptyList(),
  val pncs: List<String> = emptyList(),
  val sentenceDates: List<LocalDate> = emptyList(),
  val overrideMarker: UUID? = null,
  val overrideScopes: List<UUID> = emptyList(),
  val sourceSystem: SourceSystemType? = null,
  val eventType: CPRLogEvents,
  val recordMergedTo: Long? = null,
  val clusterComposition: List<ValidCluster>? = null,
  val statusReason: UUIDStatusReasonType? = null,
  val eventTimestamp: LocalDateTime = LocalDateTime.now(),
) {
  companion object {
    fun from(
      eventType: CPRLogEvents,
      personEntity: PersonEntity,
      personKeyEntity: PersonKeyEntity? = null,
      clusterComposition: List<ValidCluster>? = null,
    ): RecordEventLog {
      val aliases: List<PseudonymEntity> = personEntity.getAliases()
      return RecordEventLog(
        sourceSystemId = personEntity.extractSourceSystemId(),
        matchId = personEntity.matchId,
        personUUID = personKeyEntity?.personUUID ?: personEntity.personKey?.personUUID,
        uuidStatusType = personKeyEntity?.status ?: personEntity.personKey?.status,
        firstName = personEntity.getPrimaryName().firstName,
        middleNames = personEntity.getPrimaryName().middleNames,
        lastName = personEntity.getPrimaryName().lastName,
        dateOfBirth = personEntity.getPrimaryName().dateOfBirth,
        firstNameAliases = aliases.mapNotNull { it.firstName },
        lastNameAliases = aliases.mapNotNull { it.lastName },
        dateOfBirthAliases = aliases.mapNotNull { it.dateOfBirth },
        postcodes = personEntity.addresses.mapNotNull { it.postcode },
        cros = personEntity.references.getCROs(),
        pncs = personEntity.references.getType(IdentifierType.PNC).mapNotNull { it.identifierValue },
        sentenceDates = personEntity.sentenceInfo.mapNotNull { it.sentenceDate },
        overrideMarker = personEntity.overrideMarker,
        overrideScopes = personEntity.overrideScopes.map { it.scope },
        sourceSystem = personEntity.sourceSystem,
        eventType = eventType,
        recordMergedTo = personEntity.mergedTo,
        clusterComposition = clusterComposition,
        statusReason = personEntity.personKey?.statusReason,
      )
    }
  }
}
