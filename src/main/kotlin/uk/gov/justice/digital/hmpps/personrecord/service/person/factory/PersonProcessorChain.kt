package uk.gov.justice.digital.hmpps.personrecord.service.person.factory

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.CreatePersonProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonSearchProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.UpdatePersonProcessor

@Component
class PersonProcessorChain(
  private val createPersonProcessor: CreatePersonProcessor,
  private val updatePersonProcessor: UpdatePersonProcessor,
  private val personSearchProcessor: PersonSearchProcessor
) {

  private var context = PersonContext()

  fun find(block: (PersonSearchProcessor) -> PersonEntity?): PersonProcessorChain {
    context.personEntity = block(personSearchProcessor)
    return this
  }

  fun onCreate(block: (CreatePersonProcessor) -> PersonEntity?): PersonProcessorChain {
    when {
      context.personEntity == PersonEntity.empty -> {
        context.personEntity = block(createPersonProcessor)
      }
    }
    return this
  }

  fun onUpdate(block: (UpdatePersonProcessor) -> PersonEntity?): PersonProcessorChain {
    when {
      context.personEntity != PersonEntity.empty -> {
        context.personEntity = block(updatePersonProcessor)
      }
    }
    return this
  }

  fun result(): PersonEntity? = context.personEntity

}