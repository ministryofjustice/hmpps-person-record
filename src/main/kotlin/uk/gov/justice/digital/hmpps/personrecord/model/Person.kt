package uk.gov.justice.digital.hmpps.personrecord.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
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
) {
  companion object {

    fun from(personEntity: PersonEntity?): Person {
      return Person(
        personId = personEntity?.personId,
        // TODO need to properly define what a Person model object looks like: for now just return Person UUID
        // TODO pull data from HmctsDefendant Entity?
      )
    }
  }
}

data class OtherIdentifiers(
  @Schema(description = "CRN", example = "X340906")
  val crn: String? = null,
  @Schema(description = "PNC Number", example = "1965/0046583U")
  val pncNumber: String? = null,
)
