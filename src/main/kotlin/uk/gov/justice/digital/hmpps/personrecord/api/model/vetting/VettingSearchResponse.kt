package uk.gov.justice.digital.hmpps.personrecord.api.model.vetting

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDate

data class VettingSearchResponse(
  val name: VettingName,
  val aliases: List<Alias>,
  val addresses: List<Address>,
  val identifiers: List<Reference>,
  val sourceSystem: SourceSystemType,
  val status: String,
  @field:Schema(
    example = """[{"name":{"firstName":"John","middleNames":"John","lastName":"Doe"},"aliases":[],"addresses":[],"identifiers":[],"sourceSystem":"NOMIS","status":"HIGH_CONFIDENCE_MATCH"}]""",
  )
  var linkedRecords: List<VettingSearchResponse> = emptyList(),
)

data class VettingName(
  val firstName: String?,
  val middleNames: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
)
