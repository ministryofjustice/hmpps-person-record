package uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent

interface IEventProcessor {
  fun process(domainEvent: DomainEvent)
}

abstract class EventProcessor : IEventProcessor {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  final override fun process(domainEvent: DomainEvent) {
    LOG.debug("Entered process for ${this::class.java.name} type: ${domainEvent.eventType}")
    this.processEvent(domainEvent)
  }

  abstract fun processEvent(domainEvent: DomainEvent)
}
