package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object PrisonPersonReconciler {

  fun reconcile(incoming: Person, existing: PersonEntity?): Person = existing?.let {
    incoming.copy(
      disability = it.disability ?: incoming.disability,
    )
  } ?: incoming
}
