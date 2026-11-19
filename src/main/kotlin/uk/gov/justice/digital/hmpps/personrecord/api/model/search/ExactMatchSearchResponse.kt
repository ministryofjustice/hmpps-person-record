package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class ExactMatchSearchResponse(
  @ArraySchema(
    schema = Schema(
      description = "List of CRNs",
      example = "B123435",
    ),
  )
  val crns: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of prison numbers",
      example = "A1234BC",
    ),
  )
  val prisonNumbers: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of defendant IDs",
      example = "46caa4e5-ae06-4226-9cb6-682cb26cf025",
    ),
  )
  val defendantIds: List<String> = emptyList(),
  @ArraySchema(
    schema = Schema(
      description = "List of C_IDs",
      example = "\"1234567\"",
    ),
  )
  val cids: List<String> = emptyList(),
) {
  companion object {
    fun from(personEntities: List<PersonEntity>): ExactMatchSearchResponse = ExactMatchSearchResponse(
      crns = personEntities.mapNotNull { it.crn },
      prisonNumbers = personEntities.mapNotNull { it.prisonNumber },
      defendantIds = personEntities.mapNotNull { it.defendantId },
      cids = personEntities.mapNotNull { it.cId },
    )
  }
}
