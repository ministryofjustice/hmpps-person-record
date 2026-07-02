package uk.gov.justice.digital.hmpps.personrecord.service.type

const val PRISON_PERSON_CREATED = "prisoner-offender-search.prisoner.created"
const val PRISON_PERSON_UPDATED = "prisoner-offender-search.prisoner.updated"
const val PRISON_PERSON_MERGED = "prison-offender-events.prisoner.merged"

const val PROBATION_PERSON_CREATED = "probation-case.engagement.created"
const val PROBATION_PERSON_UPDATED = "probation-case.personal-details.updated"
const val PROBATION_PERSON_DELETION = "probation-case.engagement.deleted"
const val PROBATION_PERSON_GDPR_DELETION = "probation-case.deleted.gdpr"
const val PROBATION_PERSON_MERGED = "probation-case.merge.completed"
const val PROBATION_PERSON_UNMERGED = "probation-case.unmerge.completed"
const val PROBATION_PERSON_RECOVERED = "probation-case.engagement.recovered"
const val PROBATION_ALIAS_CREATED = "probation-case.alias.created"
const val PROBATION_ALIAS_UPDATED = "probation-case.alias.updated"
const val PROBATION_ALIAS_DELETED = "probation-case.alias.deleted"
const val PROBATION_ADDRESS_CREATED = "probation-case.address.created"
const val PROBATION_ADDRESS_UPDATED = "probation-case.address.updated"
const val PROBATION_ADDRESS_DELETED = "probation-case.address.deleted"

const val SAS_ADDRESS_UPDATED = "sas.accommodation.updated"
const val SAS_ADDRESS_DELETED = "sas.accommodation.deleted"

const val CPR_PRISON_PERSON_CREATED = "core-person-record.prison.record.created"
const val CPR_PROBATION_PERSON_CREATED = "core-person-record.probation.record.created"
const val CPR_COURT_PERSON_CREATED = "core-person-record.court.record.created"
const val CPR_PROBATION_ADDRESS_CREATED = "core-person-record.probation.address.created"
const val CPR_PROBATION_ADDRESS_UPDATED = "core-person-record.probation.address.updated"
const val CPR_PROBATION_ADDRESS_DELETED = "core-person-record.probation.address.deleted"
