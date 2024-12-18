package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedDateStatement
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedIdentifierStatement
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedStringStatement
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM

class MatchRecordTest {

  @Test
  fun `should return empty strings when building request if is null`() {
    val matchRecord = MatchRecord.from(PersonSearchCriteria(
      preparedFirstName = PreparedStringStatement("firstName", value = null),
      preparedLastName = PreparedStringStatement("lastName", value = null),
      preparedDateOfBirth = PreparedDateStatement("dateOfBirth", value = null),
      preparedIdentifiers = listOf(
        PreparedIdentifierStatement("pnc", reference = Reference(identifierType = PNC, identifierValue = null))
      ),
      sourceSystemType = COMMON_PLATFORM,
    ))
    assertThat(matchRecord.uniqueId).isNotEmpty()
    assertThat(matchRecord.firstName).isBlank()
    assertThat(matchRecord.lastname).isBlank()
    assertThat(matchRecord.pnc).isBlank()
    assertThat(matchRecord.dateOfBirth).isBlank()
  }

}