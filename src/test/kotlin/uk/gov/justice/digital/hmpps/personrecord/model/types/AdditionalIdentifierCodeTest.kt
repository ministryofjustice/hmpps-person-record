package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAdditionalIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Value
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.AAMR
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.ACC
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.AI02
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.AMRL
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.APNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.ASN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.DNOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.DOFF
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.DRL
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.IMMN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.LBCN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.LCRN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.LIFN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.MFCRN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.MSVN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.MTCRN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.NHS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.NINO
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.NOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.NPNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.OTHR
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.PARN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.PCRN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.PRNOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.PST
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.SPNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.URN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.VISO
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.XIMMN
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.XNOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.YCRN
import java.util.stream.Stream

class AdditionalIdentifierCodeTest {

  @Test
  fun `should handle null probation additional identifier code`() {
    val probationCaseAdditionalIdentifier = ProbationCaseAdditionalIdentifier(type = Value(null))
    assertThat(AdditionalIdentifierCode.from(probationCaseAdditionalIdentifier)).isNull()
  }

  @ParameterizedTest
  @MethodSource("probationAdditionalIdentifierCodes")
  fun `should map probation additional identifier codes to cpr additional identifier codes`(probationAdditionalIdentifier: String, cprAdditionalIdentifierCode: AdditionalIdentifierCode) {
    val probationCaseAdditionalIdentifier = ProbationCaseAdditionalIdentifier(type = Value(probationAdditionalIdentifier))
    assertThat(AdditionalIdentifierCode.from(probationCaseAdditionalIdentifier)).isEqualTo(cprAdditionalIdentifierCode)
  }

  companion object {

    @JvmStatic
    fun probationAdditionalIdentifierCodes(): Stream<Arguments> = Stream.of(
      Arguments.of("AAMR", AAMR),
      Arguments.of("ACC", ACC),
      Arguments.of("APNC", APNC),
      Arguments.of("AMRL", AMRL),
      Arguments.of("ASN", ASN),
      Arguments.of("URN", URN),
      Arguments.of("DRL", DRL),
      Arguments.of("DNOMS", DNOMS),
      Arguments.of("XIMMN", XIMMN),
      Arguments.of("XNOMS", XNOMS),
      Arguments.of("IMMN", IMMN),
      Arguments.of("DOFF", DOFF),
      Arguments.of("LCRN", LCRN),
      Arguments.of("LBCN", LBCN),
      Arguments.of("LIFN", LIFN),
      Arguments.of("MFCRN", MFCRN),
      Arguments.of("MTCRN", MTCRN),
      Arguments.of("MSVN", MSVN),
      Arguments.of("NINO", NINO),
      Arguments.of("NHS", NHS),
      Arguments.of("NOMS", NOMS),
      Arguments.of("OTHR", OTHR),
      Arguments.of("PCRN", PCRN),
      Arguments.of("PARN", PARN),
      Arguments.of("PST", PST),
      Arguments.of("AI02", AI02),
      Arguments.of("PRNOMS", PRNOMS),
      Arguments.of("SPNC", SPNC),
      Arguments.of("NPNC", NPNC),
      Arguments.of("VISO", VISO),
      Arguments.of("YCRN", YCRN),
    )
  }
}
