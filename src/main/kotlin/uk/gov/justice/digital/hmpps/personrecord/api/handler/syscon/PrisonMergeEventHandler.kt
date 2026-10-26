package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService

interface PrisonMergeEventHandler {
  fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody
}

@Profile("prod | preprod")
@Component
class PrisonMergeEventHandlerProd(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler?,
) : PrisonMergeEventHandler {

  @Transactional
  override fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody {
    prisonerSearchClient.getPrisoner(toPrisonNumber)?.let {
      val fromPersonEntity = personRepository.findByPrisonNumber(prisonMerge.fromPrisonNumber)!!
      val toPersonEntity = personRepository.findByPrisonNumber(toPrisonNumber)!!
      prisonReligionMergeHandler?.handleMerge(fromPersonEntity, toPersonEntity)
      mergeService.processMerge(fromPersonEntity, toPersonEntity, it)
    }
    // TODO return the actual mappings when we have them.
    return SysconPersonMergeResponseBody(toPrisonNumber, SysconAddressesAndContactsResponseBody(toPrisonNumber, emptyList(), emptyList()))
  }
}

@Profile("!preprod & !prod")
@Component
class PrisonMergeEventHandlerDev(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler?,
  private val sysconContactsAndAddressesMigrationHandler: SysconContactsAndAddressesMigrationHandler,
) : PrisonMergeEventHandler {

  @Transactional
  override fun handleMerge(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody = prisonerSearchClient.getPrisoner(toPrisonNumber)!!.let {
    val fromPersonEntity = personRepository.findByPrisonNumber(prisonMerge.fromPrisonNumber)!!
    val toPersonEntity = personRepository.findByPrisonNumber(toPrisonNumber)!!
    val personChangeChecker = PersonChangeChecker(toPersonEntity)

    prisonReligionMergeHandler?.handleMerge(fromPersonEntity, toPersonEntity)
    val sysconAddressesAndContactsResponseBody = sysconContactsAndAddressesMigrationHandler.insertAddressesAndContacts(toPersonEntity, prisonMerge.toPrisonAddressesAndContactsRequest)
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
