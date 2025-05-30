package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.recluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class Recluster(val cluster: PersonKeyEntity, val changedRecord: PersonEntity)
