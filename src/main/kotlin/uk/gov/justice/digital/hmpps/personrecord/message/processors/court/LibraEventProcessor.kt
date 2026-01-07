package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.jayway.jsonpath.JsonPath
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.queue.CourtMessagePublisher

@Component
class LibraEventProcessor(
  private val jsonMapper: JsonMapper,
  private val courtMessagePublisher: CourtMessagePublisher,
  private val transactionalLibraProcessor: TransactionalLibraProcessor,
) {

  fun processEvent(sqsMessage: SQSMessage) {
    val libraHearingEvent = jsonMapper.readValue(sqsMessage.message, LibraHearingEvent::class.java)
    val person = Person.from(libraHearingEvent)
    val personEntity = when {
      libraHearingEvent.isPerson() && person.isPerson() -> transactionalLibraProcessor.processLibraPerson(person)
      else -> null
    }
    val updatedMessage = addCprUUIDToLibra(sqsMessage.message, personEntity)
    courtMessagePublisher.publishMessage(sqsMessage, updatedMessage)
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
