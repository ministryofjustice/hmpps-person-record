package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_CLUSTER_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class ClusterDomainEventPublisherE2ETest : E2ETestBase() {

  @Nested
  inner class PersonJoinedCluster {

    @Test
    fun `should publish domain event when creating a person which joins an existing cluster`() {
      val basePerson = createRandomProbationCase()
      val existingPerson = createPersonWithNewKey(Person.from(basePerson))

      val newPerson = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = randomCrn()))
      probationCreateEventAndResponseSetup(ApiResponseSetup.from(newPerson))

      expectMessageCountMessagesOn(testOnlyCPRDomainEventsQueue, 2)
      val actualDomainEvent = receiveDomainEventMessage(CPR_CLUSTER_CHANGED) as CprClusterChanged
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_CLUSTER_CHANGED)
      assertThat(actualDomainEvent.version).isEqualTo(1)
      assertThat(actualDomainEvent.occurredAt).isNotBlank
      assertThat(actualDomainEvent.description).isEqualTo("A person record match has changed")
      val expectedPersonReference = PersonReference(
        identifiers = listOf(
          PersonIdentifier(type = "CRN", value = existingPerson.crn!!),
          PersonIdentifier(type = "CRN", value = newPerson.identifiers.crn!!),
        ),
      )
      assertThat(actualDomainEvent.personReference).isEqualTo(expectedPersonReference)
    }

    @Test
    fun `should not publish domain event when creating a person which joins a newly created cluster`() {
    }

    @Test
    fun `should publish a domain event when updating a person which results in a cluster merge`() {
    }

    @Test
    fun `should not publish domain event when updating a person which results in no cluster merge`() {
    }

    @Test
    fun `should publish domain event when performing an unmerge`() {
    }
  }

  @Nested
  inner class PersonLeftCluster {

    @Test
    fun `should publish domain event when deleting a person from a multi person cluster`() {
    }

    @Test
    fun `should not publish domain event when deleting a person from a single person cluster`() {
    }

    @Test
    fun `should publish a domain event when merging a person away from a multi person cluster`() {
    }

    @Test
    fun `should not publish a domain event when merging a person away from a single person cluster`() {
    }
  }
}
