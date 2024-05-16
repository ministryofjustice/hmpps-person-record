package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Alias
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonIdentifierEntity
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.name.Name
import uk.gov.justice.digital.hmpps.personrecord.model.person.name.Names
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.HMCTS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.util.*

data class Person(
  val personId: UUID? = null,
  val otherIdentifiers: OtherIdentifiers? = null,
  val defendantId: String? = null,
  val title: String? = null,
  val names: Names,
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
        names = Names(
          preferred = Name(
            firstName = offenderDetail.firstName,
            lastName = offenderDetail.surname,
            dateOfBirth = offenderDetail.dateOfBirth,
            type = NameType.PREFERRED,
          ),
        ),
        otherIdentifiers = OtherIdentifiers(
          crn = offenderDetail.otherIds.crn,
          pncIdentifier = PNCIdentifier.from(offenderDetail.otherIds.pncNumber),
          prisonNumber = offenderDetail.otherIds.nomsNumber,
        ),
        sourceSystemType = DELIUS,
      )
    }

    fun from(deliusOffenderDetail: DeliusOffenderDetail): Person {
      Names(
        preferred = Name(
          firstName = deliusOffenderDetail.name.forename,
          middleNames = deliusOffenderDetail.name.otherNames?.joinToString(" ") { it },
          lastName = deliusOffenderDetail.name.surname,
          dateOfBirth = deliusOffenderDetail.dateOfBirth,
          type = NameType.PREFERRED,
        ),
      )
      return Person(
        names = Names(
          preferred = Name(
            firstName = deliusOffenderDetail.name.forename,
            middleNames = deliusOffenderDetail.name.otherNames?.joinToString(" ") { it },
            lastName = deliusOffenderDetail.name.surname,
            dateOfBirth = deliusOffenderDetail.dateOfBirth,
            type = NameType.PREFERRED,
          ),
        ),
        otherIdentifiers = OtherIdentifiers(
          crn = deliusOffenderDetail.identifiers.crn,
          pncIdentifier = deliusOffenderDetail.identifiers.pnc,
        ),
        sourceSystemType = DELIUS,
      )
    }

    fun from(probationCase: ProbationCase): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(
          crn = probationCase.identifiers.crn,
          pncIdentifier = probationCase.identifiers.pnc,
        ),
        names = Names(
          preferred = Name(
            firstName = probationCase.name.firstName,
            middleNames = probationCase.name.middleNames,
            lastName = probationCase.name.lastName,
            dateOfBirth = probationCase.dateOfBirth,
            type = NameType.PREFERRED,
          ),
          aliases = probationCase.aliases?.map { Name.from(it) } ?: emptyList(),
        ),
        sourceSystemType = DELIUS,
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
        names = Names(
          preferred = Name(
            title = defendant.personDefendant?.personDetails?.title,
            firstName = defendant.personDefendant?.personDetails?.firstName,
            lastName = defendant.personDefendant?.personDetails?.lastName,
            middleNames = defendant.personDefendant?.personDetails?.middleName,
            dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
            type = NameType.PREFERRED,
          ),
          aliases = defendant.aliases?.map { Name.from(it) } ?: emptyList(),
        ),
        driverNumber = defendant.personDefendant?.driverNumber,
        arrestSummonsNumber = defendant.personDefendant?.arrestSummonsNumber,
        defendantId = defendant.id,
        masterDefendantId = defendant.masterDefendantId,
        nationalInsuranceNumber = defendant.personDefendant?.personDetails?.nationalInsuranceNumber,
        contacts = contacts,
        addresses = addresses,
        sourceSystemType = HMCTS,
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = PNCIdentifier.from(libraHearingEvent.pnc),
          croIdentifier = CROIdentifier.from(libraHearingEvent.cro),
        ),
        names = Names(
          preferred = Name(
            firstName = libraHearingEvent.name?.forename1,
            lastName = libraHearingEvent.name?.surname,
            dateOfBirth = libraHearingEvent.defendantDob,
            type = NameType.PREFERRED,
          ),
        ),
        sourceSystemType = HMCTS,
      )
    }

    fun from(prisoner: Prisoner): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(
          prisonNumber = prisoner.prisonNumber,
          pncIdentifier = prisoner.pnc,
          croIdentifier = prisoner.cro,
        ),
        names = Names(
          preferred = Name(
            firstName = prisoner.firstName,
            middleNames = prisoner.middleNames,
            lastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            type = NameType.PREFERRED,
          ),
          aliases = prisoner.aliases?.map { alias: Alias -> Name.from(alias) } ?: emptyList(),
        ),
        sourceSystemType = NOMIS,
      )
    }
  }
}

data class OtherIdentifiers(
  val crn: String? = null,
  val pncIdentifier: PNCIdentifier? = null,
  val croIdentifier: CROIdentifier? = null,
  var prisonNumber: String? = null,
)
