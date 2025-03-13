package uk.gov.justice.digital.hmpps.personrecord.api.constants

// Must follow naming convention defined in:
// https://dsdmoj.atlassian.net/wiki/spaces/PINT/pages/4542464001/ADR+-+PI0003+Access+Authority+Naming+Scheme

object Roles {
  const val SEARCH_API_READ_ONLY = "ROLE_CORE_PERSON_RECORD_API__SEARCH__RO"
  const val API_READ_ONLY = "ROLE_CORE_PERSON_RECORD_API__RO"
  const val QUEUE_ADMIN = "ROLE_QUEUE_ADMIN"
}
