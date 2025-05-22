package uk.gov.justice.digital.hmpps.personrecord.service.person.factory

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.empty
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonClusterProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonCreateProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonSearchProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonUpdateProcessor

class PersonProcessorChain(
  val context: PersonContext,
  private val personCreateProcessor: PersonCreateProcessor,
  private val personUpdateProcessor: PersonUpdateProcessor,
  private val personSearchProcessor: PersonSearchProcessor,
  private val personClusterProcessor: PersonClusterProcessor,
) {

  fun find(block: (PersonSearchProcessor) -> PersonEntity?): PersonProcessorChain {
    context.personEntity = block(personSearchProcessor)
    return this
  }

  fun exists(
    no: (PersonCreateProcessor, PersonContext) -> Unit = { _, _ -> },
    yes: (PersonUpdateProcessor, PersonContext) -> Unit = { _, _ -> }
  ): PersonProcessorChain {
    when {
      context.personEntity == empty -> no(personCreateProcessor, context)
      else -> yes(personUpdateProcessor, context)
    }
    return this
  }

  fun link(): PersonProcessorChain {
    personClusterProcessor.linkRecordToPersonKey(context)
    return this
  }

  fun result(): PersonEntity = context.personEntity!!

}