package uk.gov.justice.digital.hmpps.personrecord.service.refdata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.refdata.ReferenceDataValue
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PRISON
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode

class ReferenceDataServiceTest {

  private val referenceDataService = ReferenceDataService()

  @Test
  fun `should return all reference data grouped by type`() {
    val allReferenceData = referenceDataService.getAllReferenceData()

    assertThat(allReferenceData).containsOnlyKeys(
      "sex",
      "religion",
      "nationality",
      "country",
      "title",
      "ethnicity",
      "genderIdentity",
      "sexualOrientation",
      "addressStatus",
      "addressUsage",
      "contactType",
    )
    assertThat(allReferenceData["sex"]).hasSize(SexCode.entries.size)
    assertThat(allReferenceData["religion"]).hasSize(ReligionCode.entries.size)
    assertThat(allReferenceData["nationality"]).hasSize(NationalityCode.entries.size)
    assertThat(allReferenceData["country"]).hasSize(CountryCode.entries.size)
    assertThat(allReferenceData["title"]).hasSize(TitleCode.entries.size)
    assertThat(allReferenceData["ethnicity"]).hasSize(EthnicityCode.entries.size)
    assertThat(allReferenceData["genderIdentity"]).hasSize(GenderIdentityCode.entries.size)
    assertThat(allReferenceData["sexualOrientation"]).hasSize(SexualOrientation.entries.size)
    assertThat(allReferenceData["addressStatus"]).hasSize(AddressStatusCode.entries.size)
    assertThat(allReferenceData["addressUsage"]).hasSize(AddressUsageCode.entries.size)
    assertThat(allReferenceData["contactType"]).hasSize(ContactType.entries.size)
  }

  @Test
  fun `should include current flag for reference data with a current flag`() {
    assertThat(referenceDataService.getReferenceData("religion"))
      .isEqualTo(mapOf("religion" to ReligionCode.entries.map { ReferenceDataValue(it.name, it.description, it.current, it.applicableTo) }))
    assertThat(referenceDataService.getReferenceData("addressUsage"))
      .isEqualTo(mapOf("addressUsage" to AddressUsageCode.entries.map { ReferenceDataValue(it.name, it.description, it.current, it.applicableTo) }))
  }

  @Test
  fun `should return only prison address usages`() {
    val values = referenceDataService.getReferenceData("addressUsage", PRISON).getValue("addressUsage")

    assertThat(values.map { it.code }).containsExactly(
      "CARE", "CURFEW", "DAP", "DBA", "DBH", "DNF", "DOH", "DPH", "DSH", "DST", "DUT",
      "HDC", "HDC2", "HOME", "HOSP", "HOST", "OTHER", "RECEP", "RELEASE", "RES", "ROTL",
    )
    values.forEach { assertThat(it.applicableTo).containsExactly(PRISON) }
  }

  @Test
  fun `should filter all types for probation including shared and historical values`() {
    val all = referenceDataService.getAllReferenceData(PROBATION)
    assertThat(all.keys).containsExactlyElementsOf(referenceDataService.getAllReferenceData().keys)
    assertThat(all.getValue("contactType")).hasSize(ContactType.entries.size)
    assertThat(all.getValue("sex")).isEqualTo(SexCode.entries.map(ReferenceDataValue::from))
    assertThat(all.getValue("addressUsage").map { it.code }).contains("A02", "A01").doesNotContain("HOME", "UNKNOWN")
    assertThat(all.getValue("addressUsage").first { it.code == "A01" }.current).isFalse()
    all.values.flatten().forEach { assertThat(it.applicableTo).contains(PROBATION) }
  }

  @Test
  fun `should consistently filter single and all types for each domain`() {
    ReferenceDataDomain.entries.forEach { domain ->
      referenceDataService.getAllReferenceData(domain).forEach { (type, values) ->
        assertThat(referenceDataService.getReferenceData(type, domain)).isEqualTo(mapOf(type to values))
        values.forEach { assertThat(it.applicableTo).contains(domain) }
      }
    }
    assertThat(referenceDataService.getReferenceData("contactType", PRISON))
      .isEqualTo(mapOf("contactType" to emptyList<ReferenceDataValue>()))
  }

  @Test
  fun `should look up reference data type ignoring case`() {
    assertThat(referenceDataService.getReferenceData("CONTACTTYPE"))
      .containsOnlyKeys("contactType")
      .isEqualTo(referenceDataService.getReferenceData("contactType"))
  }

  @Test
  fun `should return the same grouped data as the all types response for each type`() {
    referenceDataService.getAllReferenceData().forEach { (type, values) ->
      assertThat(referenceDataService.getReferenceData(type)).isEqualTo(mapOf(type to values))
    }
  }

  @Test
  fun `should throw not found for unknown reference data type`() {
    val exception = assertThrows<ResourceNotFoundException> { referenceDataService.getReferenceData("unknown") }

    assertThat(exception.message).isEqualTo("unknown")
  }
}
