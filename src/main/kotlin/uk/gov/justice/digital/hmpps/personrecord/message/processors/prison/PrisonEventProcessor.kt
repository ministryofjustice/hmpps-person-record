package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_RECEIVED

@Component
class PrisonEventProcessor(
  private val telemetryService: TelemetryService,
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  private val encodingService: EncodingService,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(domainEvent: DomainEvent) {
    val prisonNumber = domainEvent.additionalInformation?.prisonNumber!!
    telemetryService.trackEvent(
      MESSAGE_RECEIVED,
      mapOf(EventKeys.EVENT_TYPE to domainEvent.eventType, EventKeys.PRISON_NUMBER to prisonNumber, EventKeys.SOURCE_SYSTEM to SourceSystemType.NOMIS.name),
    )
    encodingService.getPrisonerDetails(prisonNumber).fold(
      onSuccess = {
        it?.let {
          createUpdateService.processMessage(Person.from(it), domainEvent.eventType) {
            personRepository.findByPrisonNumberAndSourceSystem(prisonNumber)
          }
        }
      },
      onFailure = {
        log.error("Error retrieving prisoner detail: ${it.message}")
        throw it
      },
    )
  }
}
