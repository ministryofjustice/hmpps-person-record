package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkLocalDate
import uk.gov.justice.digital.hmpps.personrecord.extensions.withUkZone
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import java.time.ZonedDateTime

data class CanonicalAddress(
  @Schema(description = "CPR address Id", example = "ec4c7479-218c-4f11-a02d-edd749820679")
  val cprAddressId: String,
  @Schema(description = "Person no fixed abode", examples = ["false", "true", "null"])
  val noFixedAbode: Boolean? = null,
  @Schema(description = "Person address start date", example = "2020-02-26")
  val startDate: String? = null,
  @Schema(description = "Person address start date time", example = "2026-02-26T11:08:46.347Z")
  val startDateTime: ZonedDateTime? = null,
  @Schema(description = "Person address end date", example = "2023-07-15")
  val endDate: String? = null,
  @Schema(description = "Person address end date time", example = "2026-07-15T11:08:46.347Z")
  val endDateTime: ZonedDateTime? = null,
  @Schema(description = "Person address postcode", example = "SW1H 9AJ")
  val postcode: String? = null,
  @Schema(description = "Person address sub building name", example = "Sub building 2")
  val subBuildingName: String? = null,
  @Schema(description = "Person address building Name", example = "Main Building")
  val buildingName: String? = null,
  @Schema(description = "Person address building number", example = "102")
  val buildingNumber: String? = null,
  @Schema(description = "Person address thoroughfareName", example = "Petty France")
  val thoroughfareName: String? = null,
  @Schema(description = "Person address dependentLocality", example = "Westminster")
  val dependentLocality: String? = null,
  @Schema(description = "Person address post town", example = "London")
  val postTown: String? = null,
  @Schema(description = "Person address county", example = "Greater London")
  val county: String? = null,
  @Schema(description = "Person address country", example = "United Kingdom of Great Britain and Northern Ireland (the)")
  val country: String? = null,
  @Schema(
    description = "Person address country code",
    example = "GBR",
    allowableValues = ["AB", "AFGA", "AG", "ALBA", "ALGE", "AMER", "AN", "ANDO", "ANGOL", "ANTIG", "ARGEN", "ARME", "ASM", "AUSI", "AUST", "AZERB", "BAHA", "BAHR", "BANGL", "BARB", "BELA", "BELG", "BELI", "BENI", "BHUT", "BM", "BOLI", "BOSNI", "BRAZ", "BRIT", "BRUN", "BULG", "BURM", "BURU", "CAMB", "CAMER", "CANA", "CAVER", "CCK", "CDR", "CF", "CGB", "CHAD", "CHIL", "CHINA", "COK", "COLO", "COMO", "COND", "CONG", "CONR", "COSRI", "CROAT", "CT", "CUBA", "CXR", "CYPR", "CZEC", "DANE", "DJIB", "DOMI", "DUTCH", "EATIM", "ECUA", "EGYP", "EMIR", "EQUATO", "ERI", "ESTO", "ETHI", "FG", "FIJI", "FILIP", "FINN", "FO", "FREN", "GABO", "GAMB", "GE", "GERM", "GHAN", "GI", "GL", "GLP", "GREE", "GREN", "GRN", "GU", "GUAT", "GUIN", "GUYA", "HAIT", "HKG", "HOND", "HUNG", "HV", "ICE", "INDI", "INDO", "IRAN", "IRAQ", "IRISH", "ISRA", "ITAL", "IVOR", "JAM", "JAP", "JORD", "JSM", "KAZA", "KENY", "KN", "KOS", "KUWA", "KY", "LA", "LATV", "LEBA", "LIBE", "LIBY", "LIEC", "LITHU", "LS", "LUX", "MAC", "MACE", "MALA", "MALD", "MALT", "MAN", "MAR", "MARI", "MAUR", "MEXI", "MG", "MICR", "ML", "MLAW", "MNE", "MO", "MOLD", "MONA", "MONGO", "MORO", "MOTS", "MOZA", "MS", "MTQ", "NAMI", "NAUR", "NCL", "NEPA", "NFK", "NICA", "NIGER", "NIGERIA", "NIU", "NKOR", "NORW", "NZEA", "OMAN", "PAKN", "PALA", "PANA", "PARA", "PCN", "PERU", "PNGU", "POLE", "PORTU", "PRI", "PSE", "PYF", "QUAT", "REF", "REU", "RID", "ROMA", "RUSS", "RWAN", "SAARA", "SALV", "SAMO", "SANM", "SECR", "SENE", "SEYC", "SH", "SILE", "SING", "SKOR", "SLENE", "SLOV", "SOAFR", "SOLO", "SOMA", "SPAN", "SRB", "SRIL", "SRK", "SSUDAN", "STATE", "STLU", "STP", "SUDAN", "SURIN", "SW", "SWAZI", "SWEDE", "SWIS", "SYRI", "TA", "TAIW", "TANZ", "TCI", "THAI", "TKL", "TNGA", "TOGO", "TRIN", "TRS", "TU", "TUNI", "TURK", "TV", "UGAN", "UKRA", "UNKNOWN", "URUG", "UZBE", "VC", "VENE", "VG", "VIET", "VTC", "VY", "WA", "WAL", "YEMIN", "ZAM", "ZIM"],
  )
  val countryCode: String? = null,
  @Schema(description = "Person address uprn", example = "100120991537")
  val uprn: String? = null,
  @Schema(description = "Person address status")
  val status: CanonicalAddressStatus,
  @Schema(description = "Person address comment", example = "Some comment")
  val comment: String? = null,
  @Schema(description = "Person address comment", examples = ["false", "true", "null"])
  val typeVerified: Boolean? = null,
  @Schema(description = "List of person address usages")
  val usages: List<CanonicalAddressUsage> = emptyList(),
  @Schema(description = "List of person address contacts")
  val contacts: List<CanonicalContact> = emptyList(),
) {

  companion object {
    fun from(addressEntity: AddressEntity): CanonicalAddress = CanonicalAddress(
      cprAddressId = addressEntity.updateId!!.toString(),
      noFixedAbode = addressEntity.noFixedAbode,
      startDate = addressEntity.startDate?.toUkLocalDate()?.toString(),
      startDateTime = addressEntity.startDate?.withUkZone(),
      endDate = addressEntity.endDate?.toUkLocalDate()?.toString(),
      endDateTime = addressEntity.endDate?.withUkZone(),
      postcode = addressEntity.postcode,
      subBuildingName = addressEntity.subBuildingName,
      buildingName = addressEntity.buildingName,
      buildingNumber = addressEntity.buildingNumber,
      thoroughfareName = addressEntity.thoroughfareName,
      dependentLocality = addressEntity.dependentLocality,
      postTown = addressEntity.postTown,
      county = addressEntity.county,
      country = addressEntity.countryCode?.description,
      countryCode = addressEntity.countryCode?.name,
      uprn = addressEntity.uprn,
      status = CanonicalAddressStatus.from(addressEntity.statusCode),
      comment = addressEntity.comment,
      typeVerified = addressEntity.isVerified,
      usages = CanonicalAddressUsage.fromAddressUsageEntityList(addressEntity.usages),
      contacts = CanonicalContact.fromContactEntityList(addressEntity.contacts),
    )

    fun fromAddressEntityList(addressEntity: List<AddressEntity>): List<CanonicalAddress> = CanonicalAddressSorter.sort(addressEntity).map { from(it) }
  }
}
