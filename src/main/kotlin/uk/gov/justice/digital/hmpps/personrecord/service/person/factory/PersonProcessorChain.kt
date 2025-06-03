package uk.gov.justice.digital.hmpps.personrecord.service.person.factory

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.empty
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonClusterProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonCreateProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonLogProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonSearchProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonUpdateProcessor

class PersonProcessorChain(
  val context: PersonContext,
  private val personCreateProcessor: PersonCreateProcessor,
  private val personUpdateProcessor: PersonUpdateProcessor,
  private val personSearchProcessor: PersonSearchProcessor,
  private val personClusterProcessor: PersonClusterProcessor,
  private val personLogProcessor: PersonLogProcessor,
) {

  fun find(block: (PersonSearchProcessor) -> PersonEntity?): PersonProcessorChain {
    context.personEntity = block(personSearchProcessor)
    return this
  }

  fun exists(
    no: (PersonCreateProcessor, PersonContext) -> Unit = { _, _ -> },
    yes: (PersonUpdateProcessor, PersonContext) -> Unit = { _, _ -> },
  ): PersonProcessorChain {
    when {
      context.personEntity == empty -> {
        context.operation = PersonOperation.CREATE
        no(personCreateProcessor, context)
      }
      else -> {
        context.operation = PersonOperation.UPDATE
        yes(personUpdateProcessor, context)
      }
    }
    return this
  }

  fun hasClusterLink(
    no: (PersonClusterProcessor, PersonContext) -> Unit = { _, _ -> },
  ): PersonProcessorChain {
    when {
      context.personEntity?.personKey == null -> no(personClusterProcessor, context)
    }
    return this
  }

  fun matchingFieldsChanged(
    yes: (PersonProcessorChain, PersonContext) -> Unit = { _, _ -> },
  ): PersonProcessorChain {
    when {
      context.hasMatchingFieldsChanged -> {
        yes(this, context)
      }
    }
    return this
  }

  fun recordEventLog(): PersonProcessorChain {
    when (context.operation) {
      PersonOperation.CREATE -> personLogProcessor.logCreate(context)
      else -> personLogProcessor.logUpdate(context)
    }
    return this
  }

  fun recluster(): PersonProcessorChain {
    personClusterProcessor.recluster(context)
    return this
  }

  fun get(): PersonEntity = context.personEntity!!
}
