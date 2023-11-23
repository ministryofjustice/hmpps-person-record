package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.time.LocalDate

internal class DefendantEntityTest {

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
    val defendantEntity = DefendantEntity.from(person)

    // then
    assertThat(defendantEntity.title).isEqualTo("Mr")
    assertThat(defendantEntity.forenameOne).isEqualTo("givenName")
    assertThat(defendantEntity.surname).isEqualTo("familyName")
    assertThat(defendantEntity.defendantId).isEqualTo("defendantId")
    assertThat(defendantEntity.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(defendantEntity.crn).isEqualTo("crn1234")
    assertThat(defendantEntity.pncNumber).isEqualTo("pnc1234")
    assertThat(defendantEntity.cro).isEqualTo("cro1234")
    assertThat(defendantEntity.sex).isEqualTo("male")
    assertThat(defendantEntity.nationalityOne).isEqualTo("nationality1")
    assertThat(defendantEntity.nationalityTwo).isEqualTo("nationality2")
    assertThat(defendantEntity.addressLineOne).isEqualTo("addressLine1")
    assertThat(defendantEntity.addressLineTwo).isEqualTo("addressLine2")
    assertThat(defendantEntity.addressLineThree).isEqualTo("addressLine3")
    assertThat(defendantEntity.addressLineFour).isEqualTo("addressLine4")
    assertThat(defendantEntity.addressLineFive).isEqualTo("addressLine5")
    assertThat(defendantEntity.postcode).isEqualTo("postCode")
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
      DefendantEntity.from(person)
    }

    // Then
    assertThat(exception.message).contains("Missing defendant id")
  }
}
