package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.service.person.MergeService

interface PrisonMergeEventHandler {
  fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody
}

@Component
class PrisonMergeEventHandlerImpl(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler?,
  private val sysconPersonContactsAndAddressesInsertHandler: SysconPersonContactsAndAddressesInsertHandler,
) : PrisonMergeEventHandler {

  @Transactional
  override fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody = prisonerSearchClient.getPrisoner(toPrisonNumber)!!.let {
    val fromPersonEntity = personRepository.findByPrisonNumber(prisonMerge.fromPrisonNumber)!!
    val toPersonEntity = personRepository.findByPrisonNumber(toPrisonNumber)!!
    val personChangeChecker = PersonChangeChecker(toPersonEntity)

    prisonReligionMergeHandler?.handleMerge(fromPersonEntity, toPersonEntity)
    val sysconAddressesAndContactsResponseBody = sysconPersonContactsAndAddressesInsertHandler.insertAddressesAndContacts(toPersonEntity, prisonMerge.toPrisonAddressesAndContactsRequest)
    mergeService.processMerge(
      from = fromPersonEntity,
      to = toPersonEntity,
      person = it,
      personChangeChecker = personChangeChecker,
      childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class),
    )
    SysconPersonMergeResponseBody(toPrisonNumber, sysconAddressesAndContactsResponseBody)
  }
}
