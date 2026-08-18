package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class PrisonMergeEventProcessor(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val personService: PersonService,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler?,
) {

  @Transactional
  fun processEvent(fromPrisonNumber: String, toPrisonNumber: String) {
    prisonerSearchClient.getPrisoner(toPrisonNumber)?.let {
      val from: PersonEntity? = personRepository.findByPrisonNumber(fromPrisonNumber)
      val to = personRepository.findByPrisonNumber(toPrisonNumber)
      prisonReligionMergeHandler?.handleMerge(from, to)
      val processedTo: PersonEntity = personService.processPerson(it.doNotReclusterOnUpdate()) { to }
      mergeService.processMerge(from, processedTo)
    }
  }
}
