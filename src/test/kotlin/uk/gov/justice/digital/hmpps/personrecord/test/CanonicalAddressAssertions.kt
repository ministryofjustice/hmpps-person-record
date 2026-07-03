package uk.gov.justice.digital.hmpps.personrecord.test

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContact
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalContactType
import uk.gov.justice.digital.hmpps.personrecord.extensions.withUkZone
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity

fun assertCanonicalAddress(expected: AddressEntity, actual: CanonicalAddress) {
  assertThat(actual)
    .usingRecursiveComparison()
    .isEqualTo(expected.toCanonicalAddress())
}

fun assertCanonicalAddresses(expected: List<AddressEntity>, actual: List<CanonicalAddress>) {
  assertThat(actual)
    .usingRecursiveComparison()
    .isEqualTo(expected.map { it.toCanonicalAddress() })
}

private fun AddressEntity.toCanonicalAddress(): CanonicalAddress = CanonicalAddress(
  cprAddressId = updateId!!.toString(),
  noFixedAbode = noFixedAbode,
  startDate = startDate?.toLocalDate()?.toString(),
  startDateTime = startDate?.withUkZone()?.toLocalDateTime(),
  endDate = endDate?.toLocalDate()?.toString(),
  endDateTime = endDate?.withUkZone()?.toLocalDateTime(),
  postcode = postcode,
  subBuildingName = subBuildingName,
  buildingName = buildingName,
  buildingNumber = buildingNumber,
  thoroughfareName = thoroughfareName,
  dependentLocality = dependentLocality,
  postTown = postTown,
  county = county,
  country = countryCode?.description,
  countryCode = countryCode?.name,
  uprn = uprn,
  status = CanonicalAddressStatus.from(statusCode),
  comment = comment,
  typeVerified = isVerified,
  usages = usages.map { CanonicalAddressUsage(CanonicalAddressUsageCode.from(it.usageCode), it.active) },
  contacts = contacts.map { CanonicalContact(CanonicalContactType.from(it.contactType), it.contactValue, it.extension) },
)
