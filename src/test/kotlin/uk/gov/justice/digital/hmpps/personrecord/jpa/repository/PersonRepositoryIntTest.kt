package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import java.time.LocalDate

class PersonRepositoryIntTest : IntegrationTestBase() {

  @Test
  fun `should return null for unknown CRN`() {
    // Given
    val crn = "CRN9999"

    // When
    val personEntity = personRepository.findByOffendersCrn(crn)

    // Then
    assertThat(personEntity).isNull()
  }

  @Test
  fun `should return correct person with defendants matching pnc number`() {
    // Given
    val person = Person(otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier.from("2008/0056560Z")))

    val newPersonEntity = PersonEntity.new()
    val newDefendantEntity = DefendantEntity.from(person)
    newDefendantEntity.person = newPersonEntity
    newPersonEntity.defendants.add(newDefendantEntity)

    personRepository.saveAndFlush(newPersonEntity)
    val pncIdentifier = PNCIdentifier.from("2008/0056560Z")

    // When
    val personEntity = personRepository.findByDefendantsPncNumber(pncIdentifier)

    // Then
    assertThat(personEntity).isNotNull
    assertThat(personEntity?.defendants?.get(0)?.pncNumber).isEqualTo(pncIdentifier)
  }

  @Test
  fun `should return correct person records with matching pnc number`() {
    // Given
    val pncIdentifier = PNCIdentifier.from("2008/0056560Z")
    val person = Person(otherIdentifiers = OtherIdentifiers(pncIdentifier = pncIdentifier, crn = "CRN12345"))
    val newPersonEntity = PersonEntity.new()
    val newDefendantEntity = DefendantEntity.from(person)
    newDefendantEntity.person = newPersonEntity
    newPersonEntity.defendants.add(newDefendantEntity)
    val newOffenderEntity = OffenderEntity.from(person)
    newOffenderEntity.person = newPersonEntity
    newPersonEntity.offenders.add(newOffenderEntity)
    personRepository.saveAndFlush(newPersonEntity)

    // When
    val personEntity = personRepository.findPersonEntityByPncNumber(pncIdentifier)[0]

    // Then
    assertThat(personEntity.defendants[0].pncNumber).isEqualTo(pncIdentifier)
    assertThat(personEntity.offenders[0].crn).isEqualTo("CRN12345")
    assertThat(personEntity.offenders[0].pncNumber).isEqualTo(pncIdentifier)
  }

  @Test
  fun `should not return any person record with no matching pnc`() {
    // Given
    val pnc = PNCIdentifier.from("20230583843L")

    // When
    val personEntity = personRepository.findPersonEntityByPncNumber(pnc)

    // Then
    assertThat(personEntity).isEmpty()
  }

  @Test
  fun `should not return any person record with no matching dateOfBirth`() {
    // Given
    val dateOfBirth = LocalDate.parse("1986-12-27")

    // When
    val personEntity = personRepository.findPersonEntityByDateOfBirth(dateOfBirth)

    // Then
    assertThat(personEntity).isEmpty()
  }

  @Test
  fun `should not return any person record with matching dateOfBirth`() {
    // Given
    val dateOfBirth = LocalDate.parse("1986-12-27")

    val person = Person(dateOfBirth = dateOfBirth, otherIdentifiers = OtherIdentifiers(crn = "CRN12345"))
    val newPersonEntity = PersonEntity.new()

    val newDefendantEntity = DefendantEntity.from(person)
    newDefendantEntity.person = newPersonEntity
    newPersonEntity.defendants.add(newDefendantEntity)

    val newOffenderEntity = OffenderEntity.from(person)
    newOffenderEntity.person = newPersonEntity
    newPersonEntity.offenders.add(newOffenderEntity)

    personRepository.saveAndFlush(newPersonEntity)

    // When
    val personEntity = personRepository.findPersonEntityByDateOfBirth(dateOfBirth)[0]

    // Then
    assertThat(personEntity.defendants).hasSize(1)
    assertThat(personEntity.defendants[0].dateOfBirth).isEqualTo("1986-12-27")
    assertThat(personEntity.offenders).hasSize(1)
    assertThat(personEntity.offenders[0].dateOfBirth).isEqualTo("1986-12-27")
  }
}
