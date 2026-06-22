package uk.gov.justice.digital.hmpps.personrecord.service.type

const val PRISONER_CREATED = "prisoner-offender-search.prisoner.created"
const val PRISONER_UPDATED = "prisoner-offender-search.prisoner.updated"
const val PRISONER_MERGED = "prison-offender-events.prisoner.merged"

const val NEW_OFFENDER_CREATED = "probation-case.engagement.created"
const val OFFENDER_PERSONAL_DETAILS_UPDATED = "probation-case.personal-details.updated"

const val OFFENDER_ADDRESS_CREATED = "probation-case.address.created"
const val OFFENDER_ADDRESS_UPDATED = "probation-case.address.updated"
const val OFFENDER_ADDRESS_DELETED = "probation-case.address.deleted"
const val OFFENDER_MERGED = "probation-case.merge.completed"
const val OFFENDER_UNMERGED = "probation-case.unmerge.completed"
const val OFFENDER_GDPR_DELETION = "probation-case.deleted.gdpr"
const val OFFENDER_DELETION = "probation-case.engagement.deleted"

const val CPR_PRISON_PERSON_CREATED = "core-person-record.prison.record.created"
const val CPR_PROBATION_PERSON_CREATED = "core-person-record.probation.record.created"
const val CPR_COURT_PERSON_CREATED = "core-person-record.court.record.created"
const val CPR_PROBATION_ADDRESS_CREATED = "core-person-record.probation.address.created"
const val SAS_ADDRESS_UPDATED = "sas.accommodation.updated"
const val SAS_ADDRESS_ARRIVED = "sas.accommodation.person.arrived"
const val SAS_ADDRESS_DELETED = "sas.accommodation.deleted"
const val CPR_PROBATION_ADDRESS_UPDATED = "core-person-record.probation.address.updated"
const val CPR_PROBATION_ADDRESS_DELETED = "core-person-record.probation.address.deleted"
