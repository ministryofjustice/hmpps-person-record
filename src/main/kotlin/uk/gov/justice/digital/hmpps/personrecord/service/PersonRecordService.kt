package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.PrisonerDetails
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier

@Service
class PersonRecordService(
  val personRepository: PersonRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findPersonRecordsByPnc(pncIdentifier: PNCIdentifier): List<PersonEntity> {
    return personRepository.findPersonEntityByPncNumber(pncIdentifier)
  }
  fun createNewPersonAndDefendant(person: Person): PersonEntity {
    log.debug("Entered createNewPersonAndDefendant with pnc ${person.otherIdentifiers?.pncIdentifier}")

    val newPersonEntity = PersonEntity.new()
    val newDefendantEntity = createDefendant(person)
    newDefendantEntity.person = newPersonEntity
    newPersonEntity.defendants.add(newDefendantEntity)

    return personRepository.saveAndFlush(newPersonEntity)
  }

  private fun createDefendant(person: Person): DefendantEntity {
    val newDefendantEntity = DefendantEntity.from(person)

    val defendantAliases = DefendantAliasEntity.fromList(person.personAliases)
    defendantAliases.forEach { defendantAliasEntity -> defendantAliasEntity.defendant = newDefendantEntity }
    newDefendantEntity.aliases.addAll(defendantAliases)

    return newDefendantEntity
  }

  fun addOffenderToPerson(personEntity: PersonEntity, offenderDetail: OffenderDetail): PersonEntity {
    val offenderEntity = OffenderEntity.from(offenderDetail)

    val offenderAliases = OffenderAliasEntity.fromList(offenderDetail.offenderAliases)
    offenderAliases.forEach { offenderAliasEntity -> offenderAliasEntity.offender = offenderEntity }
    offenderEntity.aliases.addAll(offenderAliases)

    return addOffenderToPerson(personEntity, offenderEntity)
  }

  fun addPrisonerToPerson(personEntity: PersonEntity, prisonerDetails: PrisonerDetails): PersonEntity {
    val prisonerEntity = PrisonerEntity.from(prisonerDetails)
    prisonerEntity.person = personEntity
    personEntity.prisoners.add(prisonerEntity)
    return personRepository.saveAndFlush(personEntity)
  }

  fun addOffenderToPerson(personEntity: PersonEntity, offenderEntity: OffenderEntity): PersonEntity {
    offenderEntity.person = personEntity
    personEntity.offenders.add(offenderEntity)
    return personRepository.saveAndFlush(personEntity)
  }
}
