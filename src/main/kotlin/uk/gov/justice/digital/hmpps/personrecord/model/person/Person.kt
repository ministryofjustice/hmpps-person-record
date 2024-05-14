package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonIdentifierEntity
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDate
import java.util.*

data class Person(
  val personId: UUID? = null,
  val givenName: String? = null,
  val middleNames: List<String>? = emptyList(),
  val familyName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val birthPlace: String? = null,
  val birthCountry: String? = null,
  val otherIdentifiers: OtherIdentifiers? = null,
  val defendantId: String? = null,
  val title: String? = null,
  val aliases: List<Alias> = emptyList(),
  val driverNumber: String? = null,
  val arrestSummonsNumber: String? = null,
  val masterDefendantId: String? = null,
  val nationalInsuranceNumber: String? = null,
  val contacts: List<Contact> = emptyList(),
  val addresses: List<Address> = emptyList(),
  val sourceSystemType: SourceSystemType,
) {
  companion object {

    fun from(person: Person): PersonIdentifierEntity {
      return PersonIdentifierEntity(
        personId = person.personId,
      )
    }

    fun from(offenderDetail: OffenderDetail): Person {
      return Person(
        givenName = offenderDetail.firstName,
        familyName = offenderDetail.surname,
        dateOfBirth = offenderDetail.dateOfBirth,
        otherIdentifiers = OtherIdentifiers(
          crn = offenderDetail.otherIds.crn,
          pncIdentifier = PNCIdentifier.from(offenderDetail.otherIds.pncNumber),
          prisonNumber = offenderDetail.otherIds.nomsNumber,
        ),
        sourceSystemType = SourceSystemType.DELIUS,
      )
    }

    fun from(deliusOffenderDetail: DeliusOffenderDetail): Person {
      return Person(
        givenName = deliusOffenderDetail.name.forename,
        familyName = deliusOffenderDetail.name.surname,
        dateOfBirth = deliusOffenderDetail.dateOfBirth,
        otherIdentifiers = OtherIdentifiers(
          crn = deliusOffenderDetail.identifiers.crn,
          pncIdentifier = deliusOffenderDetail.identifiers.pnc,
        ),
        sourceSystemType = SourceSystemType.DELIUS,
      )
    }

    fun from(defendant: Defendant): Person {
      val contacts: List<Contact> = listOf(
        Contact.from(ContactType.HOME, defendant.personDefendant?.personDetails?.contact?.home),
        Contact.from(ContactType.MOBILE, defendant.personDefendant?.personDetails?.contact?.mobile),
        Contact.from(ContactType.EMAIL, defendant.personDefendant?.personDetails?.contact?.primaryEmail),
      )

      val addresses: MutableList<Address> = mutableListOf()
      defendant.personDefendant?.personDetails?.address?.postcode.let {
        addresses.add(
          Address(
            postcode = defendant.personDefendant?.personDetails?.address?.postcode,
          ),
        )
      }

      return Person(
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = defendant.pncId,
          croIdentifier = CROIdentifier.from(defendant.croNumber),
        ),
        givenName = defendant.personDefendant?.personDetails?.firstName,
        familyName = defendant.personDefendant?.personDetails?.lastName,
        middleNames = defendant.personDefendant?.personDetails?.middleName?.split(" "),
        dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
        driverNumber = defendant.personDefendant?.driverNumber,
        arrestSummonsNumber = defendant.personDefendant?.arrestSummonsNumber,
        defendantId = defendant.id,
        masterDefendantId = defendant.masterDefendantId,
        nationalInsuranceNumber = defendant.personDefendant?.personDetails?.nationalInsuranceNumber,
        contacts = contacts,
        addresses = addresses,
        aliases = defendant.aliases?.map { Alias.from(it) } ?: emptyList(),
        sourceSystemType = SourceSystemType.HMCTS,
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = PNCIdentifier.from(libraHearingEvent.pnc),
          croIdentifier = CROIdentifier.from(libraHearingEvent.cro),
        ),
        givenName = libraHearingEvent.name?.forename1,
        familyName = libraHearingEvent.name?.surname,
        dateOfBirth = libraHearingEvent.defendantDob,
        sourceSystemType = SourceSystemType.HMCTS,
      )
    }

    fun from(prisoner: Prisoner): Person =
      Person(
        otherIdentifiers = OtherIdentifiers(prisonNumber = prisoner.prisonNumber, pncIdentifier = prisoner.pnc, croIdentifier = prisoner.cro),
        givenName = prisoner.firstName,
        middleNames = prisoner.middleNames?.split(" ") ?: emptyList(),
        familyName = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
        sourceSystemType = NOMIS,
        aliases = prisoner.aliases ?: emptyList(),
      )
  }
}

data class OtherIdentifiers(
  val crn: String? = null,
  val pncIdentifier: PNCIdentifier? = null,
  val croIdentifier: CROIdentifier? = null,
  var prisonNumber: String? = null,
)
