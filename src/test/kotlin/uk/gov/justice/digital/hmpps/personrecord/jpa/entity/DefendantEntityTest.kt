package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.OtherIdentifiers
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
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
      middleNames = listOf("middleName1", "middleName2"),
      defendantId = "defendantId",
      dateOfBirth = dateOfBirth,
      otherIdentifiers = OtherIdentifiers(
        crn = "crn1234",
        pncIdentifier = PNCIdentifier.from("2001/0171310W"),
        croIdentifier = CROIdentifier.from("064072/62X"),
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
    assertThat(defendantEntity.firstName).isEqualTo("givenName")
    assertThat(defendantEntity.surname).isEqualTo("familyName")
    assertThat(defendantEntity.middleName).isEqualTo("middleName1 middleName2")
    assertThat(defendantEntity.defendantId).isEqualTo("defendantId")
    assertThat(defendantEntity.dateOfBirth).isEqualTo(dateOfBirth)
    assertThat(defendantEntity.crn).isEqualTo("crn1234")
    assertThat(defendantEntity.pncNumber).isEqualTo(PNCIdentifier.from("2001/0171310W"))
    assertThat(defendantEntity.cro).isEqualTo(CROIdentifier.from("064072/62X"))
    assertThat(defendantEntity.sex).isNull()
    assertThat(defendantEntity.nationalityOne).isNull()
    assertThat(defendantEntity.nationalityTwo).isNull()
    assertThat(defendantEntity.address?.addressLineOne).isEqualTo("addressLine1")
    assertThat(defendantEntity.address?.addressLineTwo).isEqualTo("addressLine2")
    assertThat(defendantEntity.address?.addressLineThree).isEqualTo("addressLine3")
    assertThat(defendantEntity.address?.addressLineFour).isEqualTo("addressLine4")
    assertThat(defendantEntity.address?.addressLineFive).isEqualTo("addressLine5")
    assertThat(defendantEntity.address?.postcode).isEqualTo("postCode")
  }
}
