package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.service.person.MergeService

interface PrisonMergeEventHandler {
  fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody
}

@Component
@Profile("dev | prod | preprod")
class PrisonMergeEventHandlerProd(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler,
  private val syncPersonContactsAndAddressesInsertHandler: SyncPersonContactsAndAddressesInsertHandler,
) : PrisonMergeEventHandler {

  @Transactional
  override fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody = prisonerSearchClient.getPrisoner(toPrisonNumber)!!.let {
    val fromPersonEntity = personRepository.findByPrisonNumber(prisonMerge.fromPrisonNumber)!!
    val toPersonEntity = personRepository.findByPrisonNumber(toPrisonNumber)!!
    val personChangeChecker = PersonChangeChecker(toPersonEntity)

    prisonReligionMergeHandler.handleMerge(fromPersonEntity, toPersonEntity)
    val sysconAddressesAndContactsResponseBody = syncPersonContactsAndAddressesInsertHandler.insertAddressesAndContacts(toPersonEntity, prisonMerge.toPrisonAddressesAndContactsRequest)
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

@Component
@Profile("!dev & !prod & !preprod")
class PrisonMergeEventHandlerNonProd(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler,
  private val syncPersonContactsAndAddressesInsertHandler: SyncPersonContactsAndAddressesInsertHandler,
  private val syncPersonPseudonymsAndReferencesInsertHandler: SyncPersonPseudonymsAndReferencesInsertHandler,
) : PrisonMergeEventHandler {

  @Transactional
  override fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody = prisonerSearchClient.getPrisoner(toPrisonNumber)!!.let {
    val fromPersonEntity = personRepository.findByPrisonNumber(prisonMerge.fromPrisonNumber)!!
    val toPersonEntity = personRepository.findByPrisonNumber(toPrisonNumber)!!
    val personChangeChecker = PersonChangeChecker(toPersonEntity)

    prisonReligionMergeHandler.handleMerge(fromPersonEntity, toPersonEntity)
    val sysconAddressesAndContactsResponseBody = syncPersonContactsAndAddressesInsertHandler.insertAddressesAndContacts(toPersonEntity, prisonMerge.toPrisonAddressesAndContactsRequest)
    val sysconAliasesAndIdentifiersResponseBody = syncPersonPseudonymsAndReferencesInsertHandler.insertPseudonymsAndReferences(toPersonEntity, prisonMerge.toPrisonAliasesAndIdentifiersRequest)
    mergeService.processMerge(
      from = fromPersonEntity,
      to = toPersonEntity,
      person = it,
      personChangeChecker = personChangeChecker,
      childrenToIgnore = setOf(AddressEntity::class, ContactEntity::class, PseudonymEntity::class, ReferenceEntity::class),
    )
    SysconPersonMergeResponseBody(toPrisonNumber, sysconAddressesAndContactsResponseBody, sysconAliasesAndIdentifiersResponseBody)
  }
}
