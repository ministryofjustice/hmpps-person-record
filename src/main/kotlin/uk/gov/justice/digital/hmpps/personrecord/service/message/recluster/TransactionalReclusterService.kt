package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

@Component
class TransactionalReclusterService(
  private val reclusterService: ReclusterService,
) {

  @Transactional
  fun triggerRecluster(person: PersonEntity) = reclusterService.recluster(person)
}
