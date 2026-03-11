package uk.gov.justice.digital.hmpps.personrecord.api.constants

// Must follow naming convention defined in:
// https://dsdmoj.atlassian.net/wiki/spaces/PINT/pages/4542464001/ADR+-+PI0003+Access+Authority+Naming+Scheme

object Roles {
  const val API_READ_ONLY = "ROLE_CORE_PERSON_RECORD_API__RO"

  const val PERSON_RECORD_ADMIN_READ_ONLY = "ROLE_CORE_PERSON_RECORD_API__ADMIN_RO"

  const val QUEUE_ADMIN = "ROLE_QUEUE_ADMIN"

  const val PERSON_RECORD_SYSCON_SYNC_WRITE = "ROLE_CORE_PERSON_RECORD_API__SYSCON_SYNC_RW"

  const val PROBATION_API_READ_WRITE = "ROLE_CORE_PERSON_RECORD_API__PROBATION_RW"
}
