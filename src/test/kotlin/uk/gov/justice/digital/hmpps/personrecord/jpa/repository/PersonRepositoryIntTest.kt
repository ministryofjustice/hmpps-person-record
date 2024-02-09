package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
import java.time.LocalDate
import java.util.UUID

class PersonRepositoryIntTest : IntegrationTestBase() {

  @BeforeEach
  fun before() {
    personRepository.deleteAll()
  }

  @Test
  fun `should return exact match for provided UUID`() {
    // Given
    val personId = UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002")
    val pncIdentifier = PNCIdentifier.from("2001/0171310W")
    val crn = "CRN1234"
    aDefendant(personId, "Iestyn", "Mahoney", pncIdentifier, crn, LocalDate.of(1965, 6, 18))

    // When
    val personEntity = personRepository.findByPersonId(personId)

    // Then
    assertThat(personEntity?.defendants).hasSize(1)
    val hmctsDefendantEntity = personEntity?.defendants?.get(0)
    assertThat(hmctsDefendantEntity?.pncNumber).isEqualTo(pncIdentifier)
    assertThat(hmctsDefendantEntity?.crn).isEqualTo(crn)
    assertThat(hmctsDefendantEntity?.forenameOne).isEqualTo("Iestyn")

    assertThat(hmctsDefendantEntity?.surname).isEqualTo("Mahoney")
    assertThat(hmctsDefendantEntity?.dateOfBirth).isEqualTo(LocalDate.of(1965, 6, 18))
  }

  @Test
  fun `should return null match for non existent UUID`() {
    // Given
    val personId = UUID.fromString("b0ff6dec-2706-11ee-be56-0242ac120002")

    // When
    val personEntity = personRepository.findByPersonId(personId)

    // Then
    assertThat(personEntity).isNull()
  }

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
    val personEntity = personRepository.findPersonEntityByPncNumber(pncIdentifier)

    // Then
    assertThat(personEntity).isNotNull
    assertThat(personEntity?.defendants!![0].pncNumber).isEqualTo(pncIdentifier)
    assertThat(personEntity.offenders[0].crn).isEqualTo("CRN12345")
    assertThat(personEntity.offenders[0].pncNumber).isEqualTo(pncIdentifier)
  }

  @Test
  fun `should not return any person record with no matching pnc`() {
    // Given
    val pnc = PNCIdentifier.from("PNC00000")

    // When
    val personEntity = personRepository.findPersonEntityByPncNumber(pnc)

    // Then
    assertThat(personEntity).isNull()
  }
}
