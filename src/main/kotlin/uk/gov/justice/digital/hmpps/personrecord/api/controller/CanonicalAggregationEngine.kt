package uk.gov.justice.digital.hmpps.personrecord.api.controller

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalEthnicity
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSex
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalTitle
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class CanonicalAggregationEngine(
  private val personRepository: PersonRepository,
) {

  fun get(personKeyEntity: PersonKeyEntity): CanonicalRecord {
    val canonicalRecord = fromAggregation(personKeyEntity)
    return canonicalRecord
  }

  fun fromAggregation(personKey: PersonKeyEntity): CanonicalRecord {
    val latestPerson = personKey.personEntities.sortedByDescending { it.lastModified }.first()
    return CanonicalRecord(
      cprUUID = personKey.personUUID.toString(),
      firstName = latestPerson.getPrimaryName().firstName,
      middleNames = latestPerson.getPrimaryName().middleNames,
      lastName = latestPerson.getPrimaryName().lastName,
      dateOfBirth = latestPerson.getPrimaryName().dateOfBirth?.toString(),
      disability = latestPerson.disability,
      interestToImmigration = latestPerson.immigrationStatus,
      title = CanonicalTitle.from(latestPerson.getPrimaryName().titleCode),
      sex = CanonicalSex.from(latestPerson.getPrimaryName().sexCode),
      sexualOrientation = CanonicalSexualOrientation.from(latestPerson.sexualOrientation),
      religion = CanonicalReligion.from(latestPerson.religion),
      ethnicity = CanonicalEthnicity.from(latestPerson.ethnicityCode),
      aliases = getAliases(latestPerson),
      addresses = getAddresses(latestPerson),
      identifiers = CanonicalIdentifiers.from(personKey.personEntities),
      nationalities = CanonicalNationality.from(latestPerson),
    )
  }

  private fun getAddresses(person: PersonEntity): List<CanonicalAddress> = emptyList()

  private fun getAliases(person: PersonEntity): List<CanonicalAlias> {
    val aliases = mutableListOf<CanonicalAlias>()

    person.personKey!!.personEntities.forEach {
      aliases.addAll(CanonicalAlias.from(it) ?: emptyList())
    }
    return aliases
  }
}
