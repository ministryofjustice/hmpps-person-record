package uk.gov.justice.digital.hmpps.personrecord.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import java.time.LocalDate
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Person(
  @Schema(description = "The unique person identifier", example = "f4165b62-d9eb-11ed-afa1-0242ac120002")
  val personId: UUID? = null,
  @Schema(description = "A person's first name", example = "Guinevere")
  val givenName: String? = null,
  @Schema(description = "A person's middle name(s)", example = "Catherine Anne")
  val middleNames: List<String>? = emptyList(),
  @Schema(description = "A person's surname", example = "Atherton")
  val familyName: String? = null,
  @Schema(description = "A person's date of birth", example = "1972-08-27")
  val dateOfBirth: LocalDate? = null,
  val otherIdentifiers: OtherIdentifiers? = null,
  val defendantId: String? = null,
  val title: String? = null,
  val addressLineOne: String? = null,
  val addressLineTwo: String? = null,
  val addressLineThree: String? = null,
  val addressLineFour: String? = null,
  val addressLineFive: String? = null,
  val postcode: String? = null,
  val sex: String? = null,
  val nationalityOne: String? = null,
  val nationalityTwo: String? = null,
  val personAliases: List<PersonAlias> = emptyList(),
  val driverNumber: String? = null,
  val arrestSummonsNumber: String? = null,
  val masterDefendantId: String? = null,
  val nationalityCode: String? = null,
  val nationalInsuranceNumber: String? = null,
  val observedEthnicityDescription: String? = null,
  val selfDefinedEthnicityDescription: String? = null,
  val homePhone: String? = null,
  val mobile: String? = null,
  val workPhone: String? = null,
  val primaryEmail: String? = null,
) {
  companion object {

    fun from(personEntity: PersonEntity?): Person {
      return Person(
        personId = personEntity?.personId,
        // TODO need to properly define what a Person model object looks like: for now just return Person UUID
        // TODO pull data from HmctsDefendant Entity?
      )
    }

    fun from(person: Person): PersonEntity {
      return PersonEntity(
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
      )
    }

    fun from(defendant: Defendant): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = PNCIdentifier.from(defendant.pncId),
          croIdentifier = CROIdentifier.from(defendant.croNumber)),
        givenName = defendant.personDefendant?.personDetails?.firstName,
        familyName = defendant.personDefendant?.personDetails?.lastName,
        middleNames = defendant.personDefendant?.personDetails?.middleName?.split(" "),
        dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
        sex = defendant.personDefendant?.personDetails?.gender,
        driverNumber = defendant.personDefendant?.driverNumber,
        arrestSummonsNumber = defendant.personDefendant?.arrestSummonsNumber,
        addressLineOne = defendant.personDefendant?.personDetails?.address?.address1,
        addressLineTwo = defendant.personDefendant?.personDetails?.address?.address2,
        addressLineThree = defendant.personDefendant?.personDetails?.address?.address3,
        addressLineFour = defendant.personDefendant?.personDetails?.address?.address4,
        addressLineFive = defendant.personDefendant?.personDetails?.address?.address5,
        postcode = defendant.personDefendant?.personDetails?.address?.postcode,
        defendantId = defendant.id,
        masterDefendantId = defendant.masterDefendantId,
        nationalityCode = defendant.personDefendant?.personDetails?.nationalityCode,
        nationalInsuranceNumber = defendant.personDefendant?.personDetails?.nationalInsuranceNumber,
        observedEthnicityDescription = defendant.ethnicity?.observedEthnicityDescription,
        selfDefinedEthnicityDescription = defendant.ethnicity?.selfDefinedEthnicityDescription,
        homePhone = defendant.personDefendant?.personDetails?.contact?.home,
        workPhone = defendant.personDefendant?.personDetails?.contact?.work,
        mobile = defendant.personDefendant?.personDetails?.contact?.mobile,
        primaryEmail = defendant.personDefendant?.personDetails?.contact?.primaryEmail,
        personAliases = defendant.aliases?.map { PersonAlias.from(it) } ?: emptyList(),
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = PNCIdentifier.from(libraHearingEvent.pnc),
          croIdentifier = CROIdentifier.from(libraHearingEvent.cro)
        ),
        givenName = libraHearingEvent.name?.forename1,
        familyName = libraHearingEvent.name?.surname,
        dateOfBirth = libraHearingEvent.defendantDob,
      )
    }

    fun from(prisoner: Prisoner): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = PNCIdentifier.from(prisoner.pncNumber),
          croIdentifier = CROIdentifier.from(prisoner.croNumber),
        ),
        givenName = prisoner.firstName,
        familyName = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
        middleNames = prisoner.middleNames?.split(" "),
        nationalityOne = prisoner.nationality,
        sex = prisoner.gender,
      )
    }
  }
}

data class OtherIdentifiers(
  @Schema(description = "CRN", example = "X340906")
  val crn: String? = null,
  @Schema(description = "PNC Number", example = "1965/0046583U")
  val pncIdentifier: PNCIdentifier? = null,
  @Schema(description = "CRO", example = "293110/23X")
  val croIdentifier: CROIdentifier? = null,
  var prisonNumber: String? = null,
)
