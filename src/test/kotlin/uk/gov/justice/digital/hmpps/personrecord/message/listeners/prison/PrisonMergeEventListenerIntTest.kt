package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_MERGE_RECORD_NOT_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_MERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MERGE_MESSAGE_RECEIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.MESSAGE_PROCESSING_FAILED
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup

class PrisonMergeEventListenerIntTest : MessagingMultiNodeTestBase() {

  private fun prisonURL(prisonNumber: String) = "/prisoner/$prisonNumber"

  @Test
  fun `should log when message processing fails`() {
    val targetPrisonNumber = randomPrisonNumber()
    val sourcePrisonNumber = randomPrisonNumber()
    stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure")
    stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure", "PrisonMergeEventProcessingWillFail")
    stub5xxResponse(prisonURL(targetPrisonNumber), "PrisonMergeEventProcessingWillFail", "failure", "PrisonMergeEventProcessingWillFail")

    val additionalInformation =
      AdditionalInformation(prisonNumber = targetPrisonNumber, sourcePrisonNumber = sourcePrisonNumber)
    val domainEvent =
      DomainEvent(eventType = PRISONER_MERGED, personReference = null, additionalInformation = additionalInformation)
    val messageId = publishDomainEvent(PRISONER_MERGED, domainEvent)

    purgeQueueAndDlq(prisonMergeEventsQueue)
    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf(
        "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
        "TARGET_PRISON_NUMBER" to targetPrisonNumber,
        "EVENT_TYPE" to PRISONER_MERGED,
        "SOURCE_SYSTEM" to NOMIS.name,
      ),
    )
    checkTelemetry(
      MESSAGE_PROCESSING_FAILED,
      mapOf(
        "MESSAGE_ID" to messageId,
        "SOURCE_SYSTEM" to NOMIS.name,
        "EVENT_TYPE" to "prisoner-offender-events.prisoner.merged",
      ),
    )
  }

  @Test
  fun `processes prisoner merge event when source record does not exist`() {
    val targetPrisonNumber = randomPrisonNumber()
    val sourcePrisonNumber = randomPrisonNumber()
    val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
    val personKeyEntity = createPersonKey()
    createPerson(
      Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS),
      personKeyEntity = personKeyEntity,
    )

    prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber = sourcePrisonNumber, target = target)

    checkTelemetry(
      MERGE_MESSAGE_RECEIVED,
      mapOf("SOURCE_PRISON_NUMBER" to sourcePrisonNumber, "TARGET_PRISON_NUMBER" to targetPrisonNumber, "EVENT_TYPE" to PRISONER_MERGED, "SOURCE_SYSTEM" to NOMIS.name),
    )
    checkTelemetry(
      CPR_MERGE_RECORD_NOT_FOUND,
      mapOf(
        "RECORD_TYPE" to "SOURCE",
        "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
        "TARGET_PRISON_NUMBER" to targetPrisonNumber,
        "SOURCE_SYSTEM" to NOMIS.name,
      ),
    )
    checkTelemetry(
      CPR_RECORD_MERGED,
      mapOf(
        "TO_UUID" to personKeyEntity.personUUID.toString(),
        "FROM_UUID" to null,
        "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
        "TARGET_PRISON_NUMBER" to targetPrisonNumber,
        "SOURCE_SYSTEM" to NOMIS.name,
      ),
    )
  }

  @Nested
  inner class SourceIsDeleted {
    @BeforeEach
    fun beforeEach() {
      stubDeletePersonMatch()
    }

    @Test
    fun `processes prisoner merge event with records with same UUID is published`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
      val personKeyEntity = createPersonKey()
      createPerson(
        Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity,
      )
      val targetPerson = createPerson(
        Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity,
      )

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber = sourcePrisonNumber, target = target)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf(
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "EVENT_TYPE" to PRISONER_MERGED,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to personKeyEntity.personUUID.toString(),
          "FROM_UUID" to personKeyEntity.personUUID.toString(),
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )

      val sourcePerson = personRepository.findByPrisonNumber(sourcePrisonNumber)
      assertThat(sourcePerson?.mergedTo).isEqualTo(targetPerson.id)
    }

    @Test
    fun `processes prisoner merge event when target record does not exist`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
      val personKeyEntity = createPersonKey()
      createPerson(
        Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity,
      )

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber = sourcePrisonNumber, target = target)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf(
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "EVENT_TYPE" to PRISONER_MERGED,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkTelemetry(
        CPR_MERGE_RECORD_NOT_FOUND,
        mapOf(
          "RECORD_TYPE" to "TARGET",
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has multiple records`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
      val personKeyEntity1 = createPersonKey()
      val personKeyEntity2 = createPersonKey()
      createPerson(
        Person(prisonNumber = randomPrisonNumber(), sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity1,
      )
      createPerson(
        Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity1,
      )
      val targetPerson = createPerson(
        Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity2,
      )

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber = sourcePrisonNumber, target = target)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf(
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "EVENT_TYPE" to PRISONER_MERGED,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to personKeyEntity2.personUUID.toString(),
          "FROM_UUID" to personKeyEntity1.personUUID.toString(),
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )

      val sourcePerson = personRepository.findByPrisonNumber(sourcePrisonNumber)
      assertThat(sourcePerson?.mergedTo).isEqualTo(targetPerson.id)
      assertThat(sourcePerson?.personKey).isNull()

      val sourceCluster = personKeyRepository.findByPersonUUID(personKeyEntity1.personUUID)
      assertThat(sourceCluster?.personEntities?.size).isEqualTo(1)

      val targetCluster = personKeyRepository.findByPersonUUID(personKeyEntity2.personUUID)
      assertThat(targetCluster?.personEntities?.size).isEqualTo(1)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source doesn't have an UUID`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
      val personKeyEntity2 = createPersonKey()
      createPerson(
        Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS),
      )
      val targetPerson = createPerson(
        Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity2,
      )

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber = sourcePrisonNumber, target = target)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf(
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "EVENT_TYPE" to PRISONER_MERGED,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to personKeyEntity2.personUUID.toString(),
          "FROM_UUID" to null,
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )

      val sourcePerson = personRepository.findByPrisonNumber(sourcePrisonNumber)
      assertThat(sourcePerson?.mergedTo).isEqualTo(targetPerson.id)
      assertThat(sourcePerson?.personKey).isNull()

      val targetCluster = personKeyRepository.findByPersonUUID(personKeyEntity2.personUUID)
      assertThat(targetCluster?.personEntities?.size).isEqualTo(1)
    }

    @Test
    fun `processes prisoner merge event with different UUIDs where source has a single record`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
      val personKeyEntity1 = createPersonKey()
      val personKeyEntity2 = createPersonKey()
      createPerson(
        Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity1,
      )
      val targetPerson = createPerson(
        Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity2,
      )

      prisonMergeEventAndResponseSetup(PRISONER_MERGED, sourcePrisonNumber = sourcePrisonNumber, target = target)

      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf(
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "EVENT_TYPE" to PRISONER_MERGED,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to personKeyEntity2.personUUID.toString(),
          "FROM_UUID" to personKeyEntity1.personUUID.toString(),
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )

      val sourcePerson = personRepository.findByPrisonNumber(sourcePrisonNumber)
      assertThat(sourcePerson?.mergedTo).isEqualTo(targetPerson.id)
      assertThat(sourcePerson?.personKey?.mergedTo).isEqualTo(targetPerson.personKey?.id)
      assertThat(sourcePerson?.personKey?.status).isEqualTo(UUIDStatusType.MERGED)
    }

    @Test
    fun `should retry on 500 error`() {
      val targetPrisonNumber = randomPrisonNumber()
      val sourcePrisonNumber = randomPrisonNumber()
      val personKeyEntity1 = createPersonKey()
      val personKeyEntity2 = createPersonKey()
      createPerson(
        Person(prisonNumber = sourcePrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity1,
      )
      createPerson(
        Person(prisonNumber = targetPrisonNumber, sourceSystem = NOMIS),
        personKeyEntity = personKeyEntity2,
      )
      stub5xxResponse(prisonURL(targetPrisonNumber), "next request will succeed", "retry")

      val target = ApiResponseSetup(prisonNumber = targetPrisonNumber)
      prisonMergeEventAndResponseSetup(
        PRISONER_MERGED,
        sourcePrisonNumber,
        target,
        scenario = "retry",
        currentScenarioState = "next request will succeed",
      )

      expectNoMessagesOnQueueOrDlq(prisonMergeEventsQueue)
      checkTelemetry(
        MERGE_MESSAGE_RECEIVED,
        mapOf(
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "EVENT_TYPE" to PRISONER_MERGED,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
      checkTelemetry(
        CPR_RECORD_MERGED,
        mapOf(
          "TO_UUID" to personKeyEntity2.personUUID.toString(),
          "FROM_UUID" to personKeyEntity1.personUUID.toString(),
          "SOURCE_PRISON_NUMBER" to sourcePrisonNumber,
          "TARGET_PRISON_NUMBER" to targetPrisonNumber,
          "SOURCE_SYSTEM" to NOMIS.name,
        ),
      )
    }
  }
}
