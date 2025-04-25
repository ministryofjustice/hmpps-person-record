package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.personrecord.client.model.PhoneNumber
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.deserializers.CurrentlyManagedDeserializer
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifierDeserializer
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifierDeserializer
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Prisoner(
  @NotBlank
  @JsonProperty("prisonerNumber")
  val prisonNumber: String,
  val title: String?,
  val firstName: String,
  val middleNames: String?,
  val lastName: String,
  val nationality: String?,
  val religion: String?,
  val ethnicity: String?,
  @JsonProperty("pncNumberCanonicalLong")
  @JsonDeserialize(using = PNCIdentifierDeserializer::class)
  val pnc: PNCIdentifier?,
  @JsonProperty("croNumber")
  @JsonDeserialize(using = CROIdentifierDeserializer::class)
  val cro: CROIdentifier?,
  val dateOfBirth: LocalDate,
  val aliases: List<PrisonerAlias> = emptyList(),
  val emailAddresses: List<EmailAddress> = emptyList(),
  val phoneNumbers: List<PhoneNumber> = emptyList(),
  val addresses: List<Address> = emptyList(),
  val identifiers: List<Identifier> = emptyList(),
  @JsonProperty("status")
  @JsonDeserialize(using = CurrentlyManagedDeserializer::class)
  val currentlyManaged: Boolean? = null,
  val allConvictedOffences: List<AllConvictedOffences>? = emptyList(),
  val gender: String? = null,
) {

  fun getHomePhone(): String? = phoneNumbers.firstOrNull { it.type?.contains("HOME") == true }?.number

  fun getMobilePhone(): String? = phoneNumbers.firstOrNull { it.type?.contains("MOB") == true }?.number

  companion object {
    fun List<Identifier>.getType(type: String): Identifier? = this.find { it.type == type }
  }
}
