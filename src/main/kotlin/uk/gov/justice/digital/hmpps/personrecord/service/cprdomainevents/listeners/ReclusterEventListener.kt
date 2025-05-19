package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.recluster.Recluster
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService

@Component
class ReclusterEventListener(
  private val reclusterService: ReclusterService,
) {

  @EventListener
  fun onRecluster(recluster: Recluster) {
    reclusterService.recluster(recluster.cluster, recluster.changedRecord)
  }
}
