package uk.gov.justice.digital.hmpps.personrecord.extensions

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC

fun List<ReferenceEntity>.getCROs(): List<String> = this.getType(CRO)
fun List<ReferenceEntity>.getPNCs(): List<String> = this.getType(PNC)

fun List<ReferenceEntity>.getType(type: IdentifierType): List<String> = this.filter { it.identifierType == type }.mapNotNull { it.identifierValue }
