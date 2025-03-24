package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonMatchReclusterService(
  private val personMatchService: PersonMatchService
) {

  fun recluster(personKeyEntity: PersonKeyEntity) {

  }

}