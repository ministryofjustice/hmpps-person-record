package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.time.LocalDate

internal class HmctsDefendantEntityTest {

  @Test
  fun `should create defendant entity given person`() {
    // should
    val dateOfBirth = LocalDate.now()
    val person = Person(
      title = "Mr",
      familyName = "familyName",
      givenName = "givenName",
      defendantId = "defendantId",
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(
        crn = "crn1234",
        pncNumber = "pnc1234",
        cro = "cro1234",
      ),
      sex = "male",
      nationalityOne = "nationality1",
      nationalityTwo = "nationality2",
      addressLineOne = "addressLine1",
      addressLineTwo = "addressLine2",
      addressLineThree = "addressLine3",
      addressLineFour = "addressLine4",
      addressLineFive = "addressLine5",
      postcode = "postCode",

    )
    // when
    val hmctsDefendantEntity = HmctsDefendantEntity.from(person)

    // then
    assertThat(hmctsDefendantEntity.title).isEqualTo("Mr")
    assertThat(hmctsDefendantEntity.forenameOne).isEqualTo("givenName")
    assertThat(hmctsDefendantEntity.surname).isEqualTo("familyName")
    assertThat(hmctsDefendantEntity.defendantId).isEqualTo("defendantId")
    assertThat(hmctsDefendantEntity.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(hmctsDefendantEntity.crn).isEqualTo("crn1234")
    assertThat(hmctsDefendantEntity.pncNumber).isEqualTo("pnc1234")
    assertThat(hmctsDefendantEntity.cro).isEqualTo("cro1234")
    assertThat(hmctsDefendantEntity.sex).isEqualTo("male")
    assertThat(hmctsDefendantEntity.nationalityOne).isEqualTo("nationality1")
    assertThat(hmctsDefendantEntity.nationalityTwo).isEqualTo("nationality2")
    assertThat(hmctsDefendantEntity.addressLineOne).isEqualTo("addressLine1")
    assertThat(hmctsDefendantEntity.addressLineTwo).isEqualTo("addressLine2")
    assertThat(hmctsDefendantEntity.addressLineThree).isEqualTo("addressLine3")
    assertThat(hmctsDefendantEntity.addressLineFour).isEqualTo("addressLine4")
    assertThat(hmctsDefendantEntity.addressLineFive).isEqualTo("addressLine5")
    assertThat(hmctsDefendantEntity.postcode).isEqualTo("postCode")
  }

  @Test
  fun `should throw validation exception for missing defendant id`() {
    // should
    val dateOfBirth = LocalDate.now()
    val person = Person(
      title = "Mr",
      familyName = "familyName",
      givenName = "givenName",
      defendantId = null,
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(
        crn = "crn1234",
        pncNumber = "pnc1234",
        cro = "cro1234",
      ),
      sex = "male",
      nationalityOne = "nationality1",
      nationalityTwo = "nationality2",
      addressLineOne = "addressLine1",
      addressLineTwo = "addressLine2",
      addressLineThree = "addressLine3",
      addressLineFour = "addressLine4",
      addressLineFive = "addressLine5",
      postcode = "postCode",
    )
    // when
    val exception = assertThrows(ValidationException::class.java) {
      HmctsDefendantEntity.from(person)
    }

    // Then
    assertThat(exception.message).contains("Missing defendant id")
  }
}
