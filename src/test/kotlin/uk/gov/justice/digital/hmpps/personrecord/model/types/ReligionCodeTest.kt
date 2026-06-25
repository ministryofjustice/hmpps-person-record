package uk.gov.justice.digital.hmpps.personrecord.model.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource

class ReligionCodeTest {
  @ParameterizedTest
  @FieldSource("prisonReligionCodes")
  fun `has a mapping for all of the NOMIS religion codes`(code: String) {
    val religionCode = ReligionCode.valueOf(code)
    assertThat(religionCode).isNotNull
  }

  private companion object {
    // This is a list of all the NOMIS religion codes. Any changes to the list below should then be reflected in
    // the prison_religion database table.
    private val prisonReligionCodes = listOf(
      "ADV",
      "AGNO",
      "APO",
      "ATHE",
      "BAHA",
      "BAPT",
      "BLAC",
      "BUDD",
      "CALV",
      "CCOG",
      "CE",
      "CHJCLDS",
      "CHRODX",
      "CHRST",
      "CHSC",
      "CINW",
      "COFE",
      "COFI",
      "COFN",
      "COFS",
      "CONG",
      "COPT",
      "CSW",
      "DRU",
      "EODX",
      "EORTH",
      "EPIS",
      "ETHO",
      "EVAN",
      "GOSP",
      "GROX",
      "HARE",
      "HIND",
      "HNDHAR",
      "HUM",
      "JAIN",
      "JEHV",
      "JEW",
      "LUTH",
      "METH",
      "MORM",
      "MOS",
      "MUSOTH",
      "NIL",
      "NONC",
      "NONP",
      "OORTH",
      "ORTH",
      "OTH",
      "PAG",
      "PAGDRU",
      "PENT",
      "PRES",
      "PROT",
      "QUAK",
      "RAST",
      "RC",
      "RUSS",
      "SALV",
      "SATN",
      "SCIE",
      "SDAY",
      "SHIA",
      "SHIN",
      "SHNTAO",
      "SIKH",
      "SPIR",
      "SUNI",
      "TAO",
      "TPRNTS",
      "UNIF",
      "UNIT",
      "UNKN",
      "UR",
      "WELS",
      "ZORO",
    )
  }
}
