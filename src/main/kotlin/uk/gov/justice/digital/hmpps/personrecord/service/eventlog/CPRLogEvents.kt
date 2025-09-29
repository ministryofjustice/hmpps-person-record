package uk.gov.justice.digital.hmpps.personrecord.service.eventlog

enum class CPRLogEvents {
  CPR_RECLUSTER_RECORD_MERGED,
  CPR_RECLUSTER_UUID_MERGED,
  CPR_RECORD_CREATED,
  CPR_RECORD_DELETED,
  CPR_RECORD_MERGED,
  CPR_RECORD_UNMERGED,
  CPR_RECORD_UPDATED,
  CPR_UUID_CREATED,
  CPR_UUID_DELETED,
  CPR_UUID_MERGED,
  CPR_NEEDS_ATTENTION_TO_ACTIVE,
  CPR_RECORD_SEEDED,

  // Deprecated Events. Still kept in enum so we can still decode old events.
  @Deprecated("Replaced by setting NEEDS ATTENTION on create / update")
  CPR_RECLUSTER_NEEDS_ATTENTION,

  @Deprecated("Replaced by setting NEEDS ATTENTION on create")
  CPR_RECORD_CREATED_NEEDS_ATTENTION,
}
