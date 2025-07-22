package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC

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
      example = "0700000000000002536Y",
    ),
  )
  val arrestSummonsNumbers: List<String> = emptyList(),
) {
  companion object {

    fun from(personEntities: List<PersonEntity>): CanonicalIdentifiers {
      val referenceEntities = personEntities.map { it.references }.flatten()
      return CanonicalIdentifiers(
        crns = personEntities.mapNotNull { it.crn },
        prisonNumbers = personEntities.mapNotNull { it.prisonNumber },
        defendantIds = personEntities.mapNotNull { it.defendantId },
        cids = personEntities.mapNotNull { it.cId },
        cros = referenceEntities.findByIdentifierType(CRO),
        pncs = referenceEntities.findByIdentifierType(PNC),
        nationalInsuranceNumbers = referenceEntities.findByIdentifierType(NATIONAL_INSURANCE_NUMBER),
        arrestSummonsNumbers = referenceEntities.findByIdentifierType(ARREST_SUMMONS_NUMBER),
        driverLicenseNumbers = referenceEntities.findByIdentifierType(DRIVER_LICENSE_NUMBER),
      )
    }

    fun from(personEntity: PersonEntity): CanonicalIdentifiers {
      val referenceEntities = personEntity.references
      return CanonicalIdentifiers(
        crns = personEntity.personKey!!.personEntities.mapNotNull { it.crn },
        prisonNumbers = personEntity.personKey!!.personEntities.mapNotNull { it.prisonNumber },
        defendantIds = personEntity.personKey!!.personEntities.mapNotNull { it.defendantId },
        cids = personEntity.personKey!!.personEntities.mapNotNull { it.cId },
        cros = referenceEntities.findByIdentifierType(CRO),
        pncs = referenceEntities.findByIdentifierType(PNC),
        nationalInsuranceNumbers = referenceEntities.findByIdentifierType(NATIONAL_INSURANCE_NUMBER),
        arrestSummonsNumbers = referenceEntities.findByIdentifierType(ARREST_SUMMONS_NUMBER),
        driverLicenseNumbers = referenceEntities.findByIdentifierType(DRIVER_LICENSE_NUMBER),
      )
    }

    private fun List<ReferenceEntity>.findByIdentifierType(identifierType: IdentifierType): List<String> = this.filter { it.identifierType == identifierType }.mapNotNull { it.identifierValue }
  }
}
