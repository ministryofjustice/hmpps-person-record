package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class CanonicalIdentifiers(
  @ArraySchema(
    schema = Schema(
      description = "List of CRNs",
      example = "B123435",
    ),
  )
  val crns: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of prison numbers",
      example = "A1234BC",
    ),
  )
  val prisonNumbers: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of defendant IDs",
      example = "46caa4e5-ae06-4226-9cb6-682cb26cf025",
    ),
  )
  val defendantIds: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of C_IDs",
      example = "\"1234567\"",
    ),
  )
  val cids: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of PNCs",
      example = "2000/1234567A",
    ),
  )
  val pncs: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of CROs",
      example = "123456/00A",
    ),
  )
  val cros: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of national insurance numbers",
      example = "QQ123456B",
    ),
  )
  val nationalInsuranceNumbers: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of driver license numbers",
      example = "SMITH840325J912",
    ),
  )
  val driverLicenseNumbers: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of arrest summons numbers",
      example = "",
    ),
  )
  val arrestSummonsNumbers: List<String> = emptyList(),
) {
  companion object {

    fun from(latestPerson: PersonEntity, personEntities: List<PersonEntity>): CanonicalIdentifiers = CanonicalIdentifiers(
      crns = personEntities.mapNotNull { it.crn },
      prisonNumbers = personEntities.mapNotNull { it.prisonNumber },
      defendantIds = personEntities.mapNotNull { it.defendantId },
      cids = personEntities.mapNotNull { it.cId },
      cros = latestPerson.references.getType(IdentifierType.CRO).mapNotNull { it.identifierValue },
      pncs = latestPerson.references.getType(IdentifierType.PNC).mapNotNull { it.identifierValue },
      nationalInsuranceNumbers = latestPerson.references.getType(IdentifierType.NATIONAL_INSURANCE_NUMBER).mapNotNull { it.identifierValue },
      arrestSummonsNumbers = latestPerson.references.getType(IdentifierType.ARREST_SUMMONS_NUMBER).mapNotNull { it.identifierValue },
      driverLicenseNumbers = latestPerson.references.getType(IdentifierType.DRIVER_LICENSE_NUMBER).mapNotNull { it.identifierValue },
    )
  }
}
