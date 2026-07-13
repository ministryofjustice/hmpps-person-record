package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema

data class CanonicalReligion(
  @Schema(
    description = "Person religion code",
    example = "Christianity",
    allowableValues = ["ADV", "AGNO", "APO", "ATHE", "BAHA", "BAPT", "BLAC", "BUDD", "CALV", "CCOG", "CE", "CHJCLDS", "CHRODX", "CHRST", "CHSC", "CINW", "COFE", "COFI", "COFN", "COFS", "CONG", "COPT", "CSW", "DRU", "EODX", "EORTH", "EPIS", "ETHO", "EVAN", "GOSP", "GROX", "HARE", "HIND", "HNDHAR", "HUM", "JAIN", "JEHV", "JEW", "LUTH", "METH", "MORM", "MOS", "MUSOTH", "NIL", "NONC", "NONP", "OORTH", "ORTH", "OTH", "PAG", "PAGDRU", "PENT", "PRES", "PROT", "QUAK", "RAST", "RC", "RUSS", "SALV", "SATN", "SCIE", "SDAY", "SHIA", "SHIN", "SHNTAO", "SIKH", "SPIR", "SUNI", "TAO", "TPRNTS", "UNIF", "UNIT", "UNKN", "UR", "WELS", "ZORO"],
  )
  val code: String? = null,
  @Schema(description = "Person religion description", example = "Christianity")
  val description: String? = null,

) {
  companion object {

    fun from(religion: String?) = CanonicalReligion(
      code = religion,
      description = religion,
    )
  }
}
