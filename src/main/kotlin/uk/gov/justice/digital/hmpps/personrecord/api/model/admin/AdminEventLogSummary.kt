package uk.gov.justice.digital.hmpps.personrecord.api.model.admin

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import java.util.UUID

data class AdminEventLogSummary(
  val uuid: String,
  val eventLogs: List<AdminEventLogDetails>,
) {
  companion object {
    fun from(uuid: UUID, eventLogs: List<EventLogEntity>): AdminEventLogSummary = AdminEventLogSummary(
      uuid = uuid.toString(),
      eventLogs = eventLogs.map { AdminEventLogDetails.from(it) },
    )
  }
}

data class AdminEventLogDetails(
  val uuidStatusType: String?,
  val firstName: String?,
  val firstNameAliases: List<String>,
  val middleNames: String?,
  val lastName: String?,
  val lastNameAliases: List<String>,
  val dateOfBirth: String?,
  val dateOfBirthAliases: List<String>,
  val postcodes: List<String>,
  val pncs: List<String>,
  val cros: List<String>,
  val sourceSystem: String?,
  val sourceSystemId: String?,
  val masterDefendantId: String?,
  val eventType: String,
  val recordMergedTo: Long?,
  val eventTimestamp: String,
  val sentenceDates: List<String>,
  val overrideMarker: String?,
  val overrideScopes: List<String>,

) {
  companion object {
    fun from(eventLogEntity: EventLogEntity): AdminEventLogDetails = AdminEventLogDetails(
      uuidStatusType = eventLogEntity.uuidStatusType?.name,
      firstName = eventLogEntity.firstName,
      firstNameAliases = eventLogEntity.firstNameAliases,
      middleNames = eventLogEntity.middleNames,
      lastName = eventLogEntity.lastName,
      lastNameAliases = eventLogEntity.lastNameAliases,
      dateOfBirth = eventLogEntity.dateOfBirth?.toString(),
      dateOfBirthAliases = eventLogEntity.dateOfBirthAliases.map { it.toString() },
      postcodes = eventLogEntity.postcodes,
      pncs = eventLogEntity.pncs,
      cros = eventLogEntity.cros,
      sourceSystem = eventLogEntity.sourceSystem?.name,
      sourceSystemId = eventLogEntity.sourceSystemId,
      masterDefendantId = eventLogEntity.masterDefendantId,
      eventType = eventLogEntity.eventType.name,
      recordMergedTo = eventLogEntity.recordMergedTo,
      eventTimestamp = eventLogEntity.eventTimestamp.toString(),
      sentenceDates = eventLogEntity.sentenceDates.map { it.toString() },
      overrideMarker = eventLogEntity.overrideMarker?.toString(),
      overrideScopes = eventLogEntity.overrideScopes.map { it.toString() },
    )
  }
}
