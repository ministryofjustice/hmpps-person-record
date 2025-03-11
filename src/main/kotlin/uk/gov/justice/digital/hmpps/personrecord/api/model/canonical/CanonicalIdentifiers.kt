package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

data class CanonicalIdentifiers(
  @Schema(description = "List of CRNs", example = "['B123456']")
  val crns: List<String>,
  @Schema(description = "List of prisoner numbers", example = "['B123456']")
  val prisonNumbers: List<String>,
  @Schema(description = "List of defendant IDs", example = "['B123456']")
  val defendantIds: List<String>,
  @Schema(description = "List of C_IDs", example = "['B123456']")
  val cids: List<String>,
  @Schema(description = "List of PNCs", example = "['B123456']")
  val pncs: List<String>,
  @Schema(description = "List of CROs", example = "['B123456']")
  val cros: List<String>,
  @Schema(description = "List of national insurance numbers", example = "['B123456']")
  val nationalInsuranceNumbers: List<String>,
  @Schema(description = "List of driver License numbers", example = "['B123456']")
  val driverLicenseNumbers: List<String>,
  @Schema(description = "List of arrest summon numbers", example = "['B123456']")
  val arrestSummonsNumbers: List<String>,
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
