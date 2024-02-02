package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
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
        pncIdentifier = PNCIdentifier("2001/0171310W"),
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
    assertThat(defendantEntity.pncNumber).isEqualTo(PNCIdentifier("2001/0171310W"))
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
}
