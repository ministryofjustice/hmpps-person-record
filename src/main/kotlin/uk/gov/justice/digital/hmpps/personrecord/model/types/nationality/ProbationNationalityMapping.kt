@file:Suppress("ktlint:standard:no-wildcard-imports")

package uk.gov.justice.digital.hmpps.personrecord.model.types.nationality

val PROBATION_NATIONALITY_MAPPING: Map<String, NationalityCode> = NationalityCode.entries.associateBy { it.name }
