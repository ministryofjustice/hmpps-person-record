package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CircularMergeException
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.isMerged
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.format.EncodingService
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonFactory
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonProcessorChain
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors.PersonClusterProcessor

@Component
class ProbationMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val encodingService: EncodingService,
  private val personFactory: PersonFactory,
) {

  fun processEvent(mergeDomainEvent: DomainEvent) {
    encodingService.getProbationCase(mergeDomainEvent.additionalInformation?.targetCrn!!) {
      it?.let {
        val from: PersonEntity? = personRepository.findByCrn(mergeDomainEvent.additionalInformation.sourceCrn!!)
        val to: PersonEntity = personFactory.from(Person.from(it))
          .find { searchProcessor -> searchProcessor.findByCrn(mergeDomainEvent.additionalInformation.targetCrn) }
          .exists(
            no = { createProcessor, ctx -> createProcessor.createPersonEntity(ctx) },
            yes = { updateProcessor, ctx -> updateProcessor.updatePersonEntity(ctx) }
          )
          .hasClusterLink(
            no = { clusterProcessor, ctx -> handleCircularMerge(clusterProcessor, ctx)}
          )
          .recordEventLog()
          .get()
        mergeService.processMerge(from, to)
      }
    }
  }

  private fun handleCircularMerge(clusterProcessor: PersonClusterProcessor, ctx: PersonContext) {
    when {
      ctx.personEntity!!.isMerged()-> throw CircularMergeException()
      else -> clusterProcessor.linkRecordToPersonKey(ctx)
    }
  }
}
