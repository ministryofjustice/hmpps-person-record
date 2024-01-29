package uk.gov.justice.digital.hmpps.personrecord.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
import java.time.LocalDate
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        otherIdentifiers = OtherIdentifiers(crn = offenderDetail.otherIds.crn, pncIdentifier = PNCIdentifier(offenderDetail.otherIds.pncNumber))
      )
    }

    fun from(defendant: Defendant): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier(defendant.pncId), cro = defendant.croNumber),
        givenName = defendant.personDefendant?.personDetails?.firstName,
        familyName = defendant.personDefendant?.personDetails?.lastName,
        dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(pncIdentifier = PNCIdentifier(libraHearingEvent.pnc), cro = libraHearingEvent.cro),
        givenName = libraHearingEvent.name?.forename1,
        familyName = libraHearingEvent.name?.surname,
        dateOfBirth = libraHearingEvent.defendantDob,
      )
    }

    fun from(prisoner: Prisoner): Person {
      return Person(
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = PNCIdentifier(prisoner.pncNumber),
          cro = prisoner.croNumber,
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
  val cro: String? = null,
)
