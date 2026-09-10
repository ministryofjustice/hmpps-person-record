package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

interface PrisonMergeEventProcessor {
  fun processEvent(fromPrisonNumber: String, toPrisonNumber: String)
}

@Component
@Profile("preprod | prod")
class PrisonMergeEventProcessorProd(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val personService: PersonService,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler?,
) : PrisonMergeEventProcessor {

  @Transactional
  override fun processEvent(fromPrisonNumber: String, toPrisonNumber: String) {
    prisonerSearchClient.getPrisoner(toPrisonNumber)?.let {
      val from: PersonEntity? = personRepository.findByPrisonNumber(fromPrisonNumber)
      val to = personRepository.findByPrisonNumber(toPrisonNumber)
      prisonReligionMergeHandler?.handleMerge(from, to)
      val processedTo: PersonEntity = personService.processPerson(it.doNotReclusterOnUpdate()) { to }
      mergeService.processMerge(from, processedTo)
    }
  }
}

@Component
@Profile("!preprod & !prod")
class PrisonMergeEventProcessorDev(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val personService: PersonService,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler?,
) : PrisonMergeEventProcessor {

  @Transactional
  override fun processEvent(fromPrisonNumber: String, toPrisonNumber: String) {
    prisonerSearchClient.getPrisoner(toPrisonNumber)?.let {
      val from: PersonEntity? = personRepository.findByPrisonNumber(fromPrisonNumber)
      val to = personRepository.findByPrisonNumber(toPrisonNumber)
      prisonReligionMergeHandler?.handleMerge(from, to)
      val processedTo: PersonEntity = personService.processPerson(it.doNotReclusterOnUpdate(), setOf(PseudonymEntity::class)) { to }
      mergeService.processMerge(from, processedTo)
    }
  }
}
