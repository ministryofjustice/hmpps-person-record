package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value

class SexualOrientationTest {

  @Test
  fun `retains null value for probation sexualOrientation`() {
    assertThat(SexualOrientation.from(ProbationCase(name = ProbationCaseName(), identifiers = Identifiers()))).isNull()
  }

  @Test
  fun `empty string treated as unknown value for probation sexualOrientation`() {
    assertThat(
      SexualOrientation.from(
        ProbationCase(
          name = ProbationCaseName(),
          identifiers = Identifiers(),
          sexualOrientation = Value(
            "",
          ),
        ),
      ),
    ).isEqualTo(SexualOrientation.UNKNOWN)
  }

  @Test
  fun `should use default value if sexualOrientation is not recognised`() {
    assertThat(
      SexualOrientation.from(
        ProbationCase(
          name = ProbationCaseName(),
          sexualOrientation = Value("unsupported"),
          identifiers = Identifiers(),
        ),
      ),
    ).isEqualTo(SexualOrientation.UNKNOWN)
  }
}
