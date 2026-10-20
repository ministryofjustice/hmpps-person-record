package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionMergeHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconContactsAndAddressesMigrationHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAddressesAndContactsResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconPersonMergeResponseBody
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.MergeService

interface PrisonMergeEventProcessor {
  fun processEvent(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody
}

@Profile("prod | preprod")
@Component
class PrisonMergeEventProcessorProd(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler?,
) : PrisonMergeEventProcessor {

  @Transactional
  override fun processEvent(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody {
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
class PrisonMergeEventProcessorDev(
  private val personRepository: PersonRepository,
  private val mergeService: MergeService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val prisonReligionMergeHandler: PrisonReligionMergeHandler?,
  private val sysconContactsAndAddressesMigrationHandler: SysconContactsAndAddressesMigrationHandler,
) : PrisonMergeEventProcessor {

  @Transactional
  override fun processEvent(toPrisonNumber: String, prisonMerge: PrisonMerge): SysconPersonMergeResponseBody = prisonerSearchClient.getPrisoner(toPrisonNumber)!!.let {
    val fromPersonEntity = personRepository.findByPrisonNumber(prisonMerge.fromPrisonNumber)!!
    val toPersonEntity = personRepository.findByPrisonNumber(toPrisonNumber)!!
    prisonReligionMergeHandler?.handleMerge(fromPersonEntity, toPersonEntity)
    val sysconAddressesAndContactsResponseBody = sysconContactsAndAddressesMigrationHandler.handleInsert(toPrisonNumber, prisonMerge.toPrisonAddressesAndContactsRequest, false)
    mergeService.processMerge(fromPersonEntity, toPersonEntity, it, setOf(AddressEntity::class, ContactEntity::class))
    SysconPersonMergeResponseBody(toPrisonNumber, sysconAddressesAndContactsResponseBody)
  }
}
