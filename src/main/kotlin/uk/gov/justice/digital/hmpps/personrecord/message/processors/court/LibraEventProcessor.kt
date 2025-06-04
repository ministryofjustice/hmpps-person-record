package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.jsonpath.JsonPath
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.CourtMessagePublisher

@Component
class LibraEventProcessor(
  private val objectMapper: ObjectMapper,
  private val courtMessagePublisher: CourtMessagePublisher,
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
) {

  fun processEvent(sqsMessage: SQSMessage) {
    val libraHearingEvent = objectMapper.readValue<LibraHearingEvent>(sqsMessage.message)
    val person = Person.from(libraHearingEvent)
    val personEntity = when {
      libraHearingEvent.isPerson() && person.isPerson() -> processLibraPerson(person)
      else -> null
    }
    val updatedMessage = addCprUUIDToLibra(sqsMessage.message, personEntity)
    courtMessagePublisher.publishMessage(sqsMessage, updatedMessage)
  }

  private fun processLibraPerson(person: Person): PersonEntity = createUpdateService.processPerson(person) {
    person.cId?.let {
      personRepository.findByCId(it)
    }
  }

  private fun addCprUUIDToLibra(
    message: String,
    defendant: PersonEntity?,
  ): String {
    val messageParser = JsonPath.parse(message)

    defendant?.personKey?.personUUID?.let {
      messageParser.put(
        "$",
        "cprUUID",
        it.toString(),
      )
    }

    return messageParser.jsonString()
  }
}
