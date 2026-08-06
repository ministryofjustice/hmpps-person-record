package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.search.SearchMatchStatus.Companion.toSearchStatus
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import java.time.LocalDate

data class PersonSearchResponse(val data: List<SearchData>)

data class SearchData(
  val name: SearchName,
  val aliases: List<CanonicalAlias>,
  val addresses: List<CanonicalAddress>,
  val identifiers: List<SearchIdentifier>,
  val sourceSystem: SourceSystemType,
  val status: SearchMatchStatus,
  @field:Schema(example = """[{"name":{"firstName":"asdfra","middleNames":"fuemsa","lastName":"bexeste","dateOfBirth":"1995-08-15"},"aliases":[{"firstName":"jerklqr","lastName":"xwkuett","middleNames":"qltzzyv","title":{"code":MR,"description":Mr},"sex":{"code":M,"description":Male}}],"addresses":[{"cprAddressId":"de75b1fc-9c93-4293-bc7e-6bf46a3175d5","noFixedAbode":false,"startDate":"1994-08-13","startDateTime":"1994-08-13T00:00:00","endDate":null,"endDateTime":null,"postcode":"CD9 5KL","subBuildingName":"subBuildingName","buildingName":"buildingName","buildingNumber":"5","thoroughfareName":"thoroughfareName","dependentLocality":null,"postTown":null,"county":null,"country":null,"countryCode":null,"uprn":null,"status":{"code":null,"description":null},"comment":null,"typeVerified":null,"usages":[],"contacts":[]},{"cprAddressId":"2e423922-cc3f-463b-a018-acdbd91f2f3d","noFixedAbode":false,"startDate":"1965-12-08","startDateTime":"1965-12-08T00:00:00","endDate":null,"endDateTime":null,"postcode":"XV9 2XO","subBuildingName":null,"buildingName":null,"buildingNumber":null,"thoroughfareName":null,"dependentLocality":null,"postTown":null,"county":null,"country":null,"countryCode":null,"uprn":null,"status":{"code":null,"description":null},"comment":null,"typeVerified":null,"usages":[],"contacts":[]}],"identifiers":[{"type":"CRO","value":"628285/81W","comment":null},{"type":"PNC","value":"1954/1844444B","comment":null}],"sourceSystem":"NOMIS","status":"HIGH_CONFIDENCE_MATCH","linkedRecords":[{"name":{"firstName":"wofqyvj","middleNames":null,"lastName":"osqqttb","dateOfBirth":"2017-01-12"},"aliases":[{"firstName":"jmjcsuw","lastName":"gpybpvf","middleNames":"zagknav","title":{"code":null,"description":null},"sex":{"code":null,"description":null}}],"addresses":[{"cprAddressId":"c611b4d4-900b-4ed5-8cce-f361704c62fa","noFixedAbode":true,"startDate":"2015-01-22","startDateTime":"2015-01-22T00:00:00","endDate":null,"endDateTime":null,"postcode":"ID8 6MV","subBuildingName":null,"buildingName":null,"buildingNumber":null,"thoroughfareName":null,"dependentLocality":null,"postTown":null,"county":null,"country":null,"countryCode":null,"uprn":null,"status":{"code":null,"description":null},"comment":null,"typeVerified":null,"usages":[],"contacts":[]},{"cprAddressId":"195dbe32-a1fc-4a33-b73a-6313eba394fa","noFixedAbode":false,"startDate":"1950-01-09","startDateTime":"1950-01-09T00:00:00","endDate":null,"endDateTime":null,"postcode":"UD8 4AG","subBuildingName":null,"buildingName":null,"buildingNumber":null,"thoroughfareName":null,"dependentLocality":null,"postTown":null,"county":null,"country":null,"countryCode":null,"uprn":null,"status":{"code":null,"description":null},"comment":null,"typeVerified":null,"usages":[],"contacts":[]}],"identifiers":[{"type":"CRO","value":"948174/19X","comment":null},{"type":"PNC","value":"2019/2379161H","comment":null}],"sourceSystem":"NOMIS","status":"HIGH_CONFIDENCE_MATCH"}]}]""")
  var linkedRecords: List<SearchData> = emptyList(),
) {
  companion object {
    fun from(personEntity: PersonEntity): SearchData {
      val mainPseudonym = personEntity.getPrimaryName()
      return SearchData(
        name = SearchName(
          firstName = mainPseudonym.firstName,
          middleNames = mainPseudonym.middleNames,
          lastName = mainPseudonym.lastName,
          dateOfBirth = mainPseudonym.dateOfBirth,
        ),
        aliases = CanonicalAlias.from(personEntity) ?: emptyList(),
        addresses = personEntity.addresses.map { CanonicalAddress.from(it) },
        identifiers = personEntity.references.map { SearchIdentifier.from(it) },
        sourceSystem = personEntity.sourceSystem,
        status = personEntity.personKey!!.status.toSearchStatus(),
      )
    }
  }
}

data class SearchName(
  val firstName: String?,
  val middleNames: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
)

data class SearchIdentifier(
  val type: IdentifierType,
  val value: String? = null,
  val comment: String? = null,
) {
  companion object {
    fun from(referenceEntity: ReferenceEntity) = SearchIdentifier(
      type = referenceEntity.identifierType,
      value = referenceEntity.identifierValue,
      comment = referenceEntity.comment,
    )
  }
}

enum class SearchMatchStatus {
  HIGH_CONFIDENCE_MATCH,
  LOW_CONFIDENCE_MATCH,
  ;

  companion object {
    fun UUIDStatusType.toSearchStatus(): SearchMatchStatus = when (this) {
      ACTIVE -> HIGH_CONFIDENCE_MATCH
      else -> LOW_CONFIDENCE_MATCH
    }
  }
}
