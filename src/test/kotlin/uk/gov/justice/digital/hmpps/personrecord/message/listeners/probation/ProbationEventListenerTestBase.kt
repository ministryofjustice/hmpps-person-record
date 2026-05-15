package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.test.randomAddressNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomDigit
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLowerCaseString
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomUprn
import uk.gov.justice.digital.hmpps.personrecord.test.randomZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressStatus
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddressUsage
import uk.gov.justice.digital.hmpps.personrecord.test.responses.probationAddress

class ProbationEventListenerTestBase : MessagingMultiNodeTestBase() {

  fun randomProbationAddress(deliusAddressId: Long? = null): ProbationAddress {
    val startDateTime = randomZonedDateTime()
    val endDateTime = startDateTime.plusYears(10)
    return ProbationAddress(
      noFixedAbode = true,
      startDateTime = startDateTime,
      endDateTime = endDateTime,
      postcode = randomPostcode(),
      fullAddress = randomFullAddress(),
      buildingName = randomName(),
      addressNumber = randomAddressNumber(),
      streetName = randomLowerCaseString(),
      district = randomName(),
      townCity = randomName(),
      county = randomName(),
      uprn = randomUprn(),
      deliusAddressId = deliusAddressId ?: randomDigit().toLong(),
      isVerified = randomBoolean(),
      notes = randomLowerCaseString(),
      status = ProbationAddressStatus(AddressStatusCode.entries.random().name, "description"),
      usage = ProbationAddressUsage(AddressUsageCode.entries.random().name, "description"),
      telephoneNumber = randomPhoneNumber(),
    )
  }

  fun insertAddress(personEntity: PersonEntity, deliusAddressId: Long?, probationAddress: ProbationAddress? = null): AddressEntity {
    val cprAddress = probationAddress?.let { Address.from(it) } ?: Address(postcode = randomPostcode(), deliusAddressId = deliusAddressId)
    val addressEntity = AddressEntity.from(cprAddress)
    addressEntity.person = personEntity
    personEntity.addresses.add(addressEntity)
    return personRepository.save(personEntity).addresses.first()
  }

  fun stubGetRequestToProbation(probationAddress: ProbationAddress, status: Int = 200) {
    stubGetRequest(
      url = "/address/${probationAddress.deliusAddressId}",
      status = status,
      body = probationAddress(
        address = ApiResponseSetupAddress(
          noFixedAbode = probationAddress.noFixedAbode,
          startDateTime = probationAddress.startDateTime,
          endDateTime = probationAddress.endDateTime,
          postcode = probationAddress.postcode,
          fullAddress = probationAddress.fullAddress,
          buildingName = probationAddress.buildingName,
          addressNumber = probationAddress.addressNumber,
          streetName = probationAddress.streetName,
          district = probationAddress.district,
          townCity = probationAddress.townCity,
          county = probationAddress.county,
          deliusAddressId = probationAddress.deliusAddressId!!,
          isVerified = probationAddress.isVerified,
          status = ApiResponseSetupAddressStatus(probationAddress.status?.code, probationAddress.status?.description),
          usage = ApiResponseSetupAddressUsage(probationAddress.usage?.code, probationAddress.usage?.description),
          uprn = probationAddress.uprn,
          notes = probationAddress.notes,
          telephoneNumber = probationAddress.telephoneNumber,
        ),
      ),
    )
  }

  fun publishProbationAddressEvent(crn: String?, probationAddressId: Long?, eventType: String) {
    publishDomainEvent(
      eventType,
      DomainEvent(
        eventType = eventType,
        detailUrl = "/address/$probationAddressId",
        personReference = PersonReference(listOf(PersonIdentifier("CRN", crn!!))),
      ),
    )
  }

  fun assertAddress(actualPersonEntity: PersonEntity, probationAddress: ProbationAddress) {
    awaitAssert {
      assertThat(actualPersonEntity.addresses.size).isEqualTo(1)
      val actualAddressEntity = actualPersonEntity.addresses.first()
      assertThat(actualAddressEntity.updateId).isNotNull()
      assertThat(actualAddressEntity.updateId!!.toString()).isNotBlank
      assertThat(actualAddressEntity.noFixedAbode).isEqualTo(probationAddress.noFixedAbode)
      assertThat(actualAddressEntity.startDate).isEqualTo(probationAddress.startDateTime)
      assertThat(actualAddressEntity.endDate).isEqualTo(probationAddress.endDateTime)
      assertThat(actualAddressEntity.postcode).isEqualTo(probationAddress.postcode)
      assertThat(actualAddressEntity.fullAddress).isEqualTo(probationAddress.fullAddress)
      assertThat(actualAddressEntity.buildingName).isEqualTo(probationAddress.buildingName)
      assertThat(actualAddressEntity.postTown).isEqualTo(probationAddress.townCity)
      assertThat(actualAddressEntity.county).isEqualTo(probationAddress.county)
      assertThat(actualAddressEntity.uprn).isEqualTo(probationAddress.uprn)
      assertThat(actualAddressEntity.deliusAddressId).isEqualTo(probationAddress.deliusAddressId)
      assertThat(actualAddressEntity.isVerified).isEqualTo(probationAddress.isVerified)
      assertThat(actualAddressEntity.usages.first().usageCode).isEqualTo(AddressUsageCode.from(probationAddress.usage!!.code))
      assertThat(actualAddressEntity.statusCode).isEqualTo(AddressStatusCode.valueOf(probationAddress.status?.code!!))
      assertThat(actualAddressEntity.buildingNumber).isEqualTo(probationAddress.addressNumber)
      assertThat(actualAddressEntity.thoroughfareName).isEqualTo(probationAddress.streetName)
      assertThat(actualAddressEntity.dependentLocality).isEqualTo(probationAddress.district)
      assertThat(actualAddressEntity.comment).isEqualTo(probationAddress.notes)
      assertThat(actualAddressEntity.contacts.first().contactType).isEqualTo(ContactType.HOME)
      assertThat(actualAddressEntity.contacts.first().contactValue).isEqualTo(probationAddress.telephoneNumber)
    }
  }
}
