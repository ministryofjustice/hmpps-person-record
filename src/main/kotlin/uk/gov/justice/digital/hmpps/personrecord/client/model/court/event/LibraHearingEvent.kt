package uk.gov.justice.digital.hmpps.personrecord.client.model.court.event

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.OptBoolean
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Address
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.DefendantType.PERSON
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifierDeserializer
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifierDeserializer
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LibraHearingEvent(
  val name: Name? = null,
  @JsonProperty("defendantDob")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", lenient = OptBoolean.TRUE)
  @JsonDeserialize(using = LocalDateDeserializer::class)
  val dateOfBirth: LocalDate? = null,
  val defendantAddress: Address? = null,
  @JsonDeserialize(using = CROIdentifierDeserializer::class)
  val cro: CROIdentifier? = CROIdentifier.from(),
  @JsonDeserialize(using = PNCIdentifierDeserializer::class)
  val pnc: PNCIdentifier? = PNCIdentifier.from(),
  val cId: String? = null,
  val defendantType: String? = null,
  val defendantSex: String? = null,
) {
  fun isPerson(): Boolean = defendantType == PERSON.value
}
