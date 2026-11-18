package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_CLUSTER_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_DELETED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PROBATION_PERSON_DELETED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
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
      val person = createRandomProbationCase()

      probationCreateEventAndResponseSetup(ApiResponseSetup.from(person))

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val actualDomainEvent = receiveDomainEventMessage(CPR_PROBATION_PERSON_CREATED) as CprPersonCreated
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_CREATED)
    }

    @Test
    fun `should publish a domain event when updating a person which results in a cluster merge`() {
      val basePerson = createRandomProbationCase()

      val personOne = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = randomCrn()))
      val personTwo = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = randomCrn()))
      val clusterOne = createPersonKey()
        .addPerson(Person.from(personOne))
        .addPerson(Person.from(personTwo))

      val personThree = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = randomCrn()))
      val personFour = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = randomCrn()))
      val clusterTwo = createPersonKey()
        .addPerson(Person.from(personThree))
        .addPerson(Person.from(personFour))

      probationUpdateEventAndResponseSetup(ApiResponseSetup.from(personThree.copy(name = ProbationCaseName(firstName = randomName()))))

      checkTelemetry(
        CPR_RECLUSTER_MERGE,
        mapOf(
          "FROM_UUID" to clusterOne.personUUID.toString(),
          "TO_UUID" to clusterTwo.personUUID.toString(),
        ),
      )

      receiveDomainEventMessage(CPR_PROBATION_PERSON_UPDATED) as CprPersonUpdated
      val actualDomainEvent = receiveDomainEventMessage(CPR_CLUSTER_CHANGED) as CprClusterChanged
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_CLUSTER_CHANGED)
      assertThat(actualDomainEvent.version).isEqualTo(1)
      assertThat(actualDomainEvent.occurredAt).isNotBlank
      assertThat(actualDomainEvent.description).isEqualTo("A person record match has changed")
      val expectedIdentifiers = listOf(
        PersonIdentifier(type = "CRN", value = personOne.identifiers.crn!!),
        PersonIdentifier(type = "CRN", value = personTwo.identifiers.crn!!),
        PersonIdentifier(type = "CRN", value = personThree.identifiers.crn!!),
        PersonIdentifier(type = "CRN", value = personFour.identifiers.crn!!),
      )
      assertThat(actualDomainEvent.personReference.identifiers).hasSameElementsAs(expectedIdentifiers)
    }

    @Test
    fun `should not publish domain event when updating a person which results in no cluster merge`() {
      val basePerson = createRandomProbationCase()

      val personOne = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = randomCrn()))
      val personTwo = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = randomCrn()))
      createPersonKey()
        .addPerson(Person.from(personOne))
        .addPerson(Person.from(personTwo))

      probationUpdateEventAndResponseSetup(ApiResponseSetup.from(personTwo.copy(name = ProbationCaseName(firstName = randomName()))))

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val actualDomainEvent = receiveDomainEventMessage(CPR_PROBATION_PERSON_UPDATED) as CprPersonUpdated
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_UPDATED)
    }

    @Test
    fun `should publish domain event when performing an unmerge which results in a multi person cluster`() {
      val fromCrn = randomCrn()
      val toCrn = randomCrn()
      val basePerson = createRandomProbationCase()

      val fromPerson = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = fromCrn))
      val toPerson = basePerson.copy(identifiers = basePerson.identifiers.copy(crn = toCrn))
      createPersonKey()
        .addPerson(Person.from(fromPerson))
        .addPerson(Person.from(toPerson))

      probationMergeEventAndResponseSetup(
        sourceCrn = fromCrn,
        targetCrn = toCrn,
        apiResponseSetup = ApiResponseSetup.from(createRandomProbationCase(toCrn)),
      )

      checkTelemetry(
        event = CPR_RECORD_MERGED,
        expected = mapOf(
          "FROM_SOURCE_SYSTEM_ID" to fromCrn,
          "TO_SOURCE_SYSTEM_ID" to toCrn,
        ),
      )

      receiveDomainEventMessage(CPR_PROBATION_PERSON_MERGED) as CprPersonMerged
      receiveDomainEventMessage(CPR_CLUSTER_CHANGED) as CprClusterChanged

      val personThreeCrn = randomCrn()
      val basePersonTwo = createRandomProbationCase()
      createPersonKey() // <- cluster 'fromPerson' will join once unmerged
        .addPerson(Person.from(basePersonTwo.copy(identifiers = basePersonTwo.identifiers.copy(crn = personThreeCrn))))

      probationUnmergeEventAndResponseSetup(
        fromCrn,
        toCrn,
        reactivatedSetup = ApiResponseSetup.from(basePersonTwo.copy(identifiers = basePersonTwo.identifiers.copy(crn = fromCrn), name = ProbationCaseName(firstName = randomName()))),
        unmergedSetup = ApiResponseSetup.from(toPerson),
      )

      checkTelemetry(
        event = CPR_RECORD_UNMERGED,
        expected = mapOf(
          "FROM_SOURCE_SYSTEM_ID" to toCrn,
          "TO_SOURCE_SYSTEM_ID" to fromCrn,
          "SOURCE_SYSTEM" to "DELIUS",
        ),
      )

      val actualDomainEvent = receiveDomainEventMessage(CPR_CLUSTER_CHANGED) as CprClusterChanged
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_CLUSTER_CHANGED)
      assertThat(actualDomainEvent.version).isEqualTo(1)
      assertThat(actualDomainEvent.occurredAt).isNotBlank
      assertThat(actualDomainEvent.description).isEqualTo("A person record match has changed")
      val expectedIdentifiers = listOf(
        PersonIdentifier(type = "CRN", value = personThreeCrn),
        PersonIdentifier(type = "CRN", value = fromCrn),
      )
      assertThat(actualDomainEvent.personReference.identifiers).hasSameElementsAs(expectedIdentifiers)
    }
  }

  @Nested
  inner class PersonLeftCluster {

    @Test
    fun `should publish domain event when deleting a person from a multi person cluster`() {
      val personOne = createRandomProbationCase()
      val personTwo = createRandomProbationCase()
      val cluster = createPersonKey()
        .addPerson(Person.from(personOne))
        .addPerson(Person.from(personTwo))

      publishProbationPersonDeletedEvent(PROBATION_PERSON_DELETED, personTwo.identifiers.crn!!)

      checkTelemetry(
        event = CPR_RECORD_DELETED,
        expected = mapOf(
          "UUID" to cluster.personUUID.toString(),
          "IS_OVERRIDE_MARKER_DELETE" to "false",
        ),
      )

      receiveDomainEventMessage(CPR_PROBATION_PERSON_DELETED) as CprPersonDeleted
      val actualDomainEvent = receiveDomainEventMessage(CPR_CLUSTER_CHANGED) as CprClusterChanged
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_CLUSTER_CHANGED)
      assertThat(actualDomainEvent.version).isEqualTo(1)
      assertThat(actualDomainEvent.occurredAt).isNotBlank
      assertThat(actualDomainEvent.description).isEqualTo("A person record match has changed")
      val expectedPersonReference = PersonReference(
        identifiers = listOf(
          PersonIdentifier(type = "CRN", value = personOne.identifiers.crn!!),
        ),
      )
      assertThat(actualDomainEvent.personReference).isEqualTo(expectedPersonReference)
    }

    @Test
    fun `should not publish domain event when deleting a person from a single person cluster`() {
      val personOne = createRandomProbationCase()
      val cluster = createPersonKey()
        .addPerson(Person.from(personOne))

      publishProbationPersonDeletedEvent(PROBATION_PERSON_DELETED, personOne.identifiers.crn!!)

      checkTelemetry(
        event = CPR_RECORD_DELETED,
        expected = mapOf(
          "UUID" to cluster.personUUID.toString(),
          "IS_OVERRIDE_MARKER_DELETE" to "false",
        ),
      )

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val actualDomainEvent = receiveDomainEventMessage(CPR_PROBATION_PERSON_DELETED) as CprPersonDeleted
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_DELETED)
    }

    @Test
    fun `should publish a domain event when merging a person in a multi person cluster`() {
      val fromCrn = randomCrn()
      val toCrn = randomCrn()
      createPersonKey()
        .addPerson(Person.from(createRandomProbationCase(fromCrn)))
        .addPerson(Person.from(createRandomProbationCase(toCrn)))

      probationMergeEventAndResponseSetup(
        sourceCrn = fromCrn,
        targetCrn = toCrn,
        apiResponseSetup = ApiResponseSetup.from(createRandomProbationCase(toCrn)),
      )

      checkTelemetry(
        event = CPR_RECORD_MERGED,
        expected = mapOf(
          "FROM_SOURCE_SYSTEM_ID" to fromCrn,
          "TO_SOURCE_SYSTEM_ID" to toCrn,
        ),
      )

      receiveDomainEventMessage(CPR_PROBATION_PERSON_MERGED) as CprPersonMerged
      val actualDomainEvent = receiveDomainEventMessage(CPR_CLUSTER_CHANGED) as CprClusterChanged
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_CLUSTER_CHANGED)
      assertThat(actualDomainEvent.version).isEqualTo(1)
      assertThat(actualDomainEvent.occurredAt).isNotBlank
      assertThat(actualDomainEvent.description).isEqualTo("A person record match has changed")
      val expectedPersonReference = PersonReference(
        identifiers = listOf(
          PersonIdentifier(type = "CRN", value = toCrn),
        ),
      )
      assertThat(actualDomainEvent.personReference).isEqualTo(expectedPersonReference)
    }

    @Test
    fun `should not publish a domain event when merging a person in a single person cluster`() {
      val fromCrn = randomCrn()
      val toCrn = randomCrn()
      createPersonKey().addPerson(Person.from(createRandomProbationCase(fromCrn)))
      createPersonKey().addPerson(Person.from(createRandomProbationCase(toCrn)))

      probationMergeEventAndResponseSetup(
        sourceCrn = fromCrn,
        targetCrn = toCrn,
        apiResponseSetup = ApiResponseSetup.from(createRandomProbationCase(toCrn)),
      )

      checkTelemetry(
        event = CPR_RECORD_MERGED,
        expected = mapOf(
          "FROM_SOURCE_SYSTEM_ID" to fromCrn,
          "TO_SOURCE_SYSTEM_ID" to toCrn,
        ),
      )

      expectOneMessageOn(testOnlyCPRDomainEventsQueue)
      val actualDomainEvent = receiveDomainEventMessage(CPR_PROBATION_PERSON_MERGED) as CprPersonMerged
      assertThat(actualDomainEvent.eventType).isEqualTo(CPR_PROBATION_PERSON_MERGED)
    }
  }
}
