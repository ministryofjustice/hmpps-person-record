package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin.cluster

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.util.UUID

class ClusterApiIntTest : WebTestBase() {

  @Nested
  inner class UUIDClusterDetail {

    @Test
    fun `should return cluster record with one record details`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())

      stubVisualiseCluster()

      val response = webTestClient.get()
        .uri(uuidClusterUrl(person.personKey?.personUUID.toString()))
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(AdminClusterDetail::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
      assertThat(response.status).isEqualTo("ACTIVE")
      assertThat(response.statusReason).isNull()
      assertThat(response.records.size).isEqualTo(1)
      assertThat(response.records[0].sourceSystem).isEqualTo("DELIUS")
      assertThat(response.records[0].sourceSystemId).isEqualTo(person.crn)
      assertThat(response.records[0].firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(response.records[0].middleName).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(response.records[0].lastName).isEqualTo(person.getPrimaryName().lastName)
    }

    @Test
    fun `should return needs attention cluster record details`() {
      val person = createPersonWithNewKey(
        createRandomProbationPersonDetails(),
        UUIDStatusType.NEEDS_ATTENTION,
        UUIDStatusReasonType.BROKEN_CLUSTER,
      )

      stubVisualiseCluster()

      val response = webTestClient.get()
        .uri(uuidClusterUrl(person.personKey?.personUUID.toString()))
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(AdminClusterDetail::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
      assertThat(response.status).isEqualTo("NEEDS_ATTENTION")
      assertThat(response.statusReason?.code).isEqualTo(UUIDStatusReasonType.BROKEN_CLUSTER.name)
      assertThat(response.statusReason?.description).isEqualTo("Some records within the cluster no longer meet the similarity threshold.")
      assertThat(response.records.size).isEqualTo(1)
      assertThat(response.records[0].sourceSystem).isEqualTo("DELIUS")
      assertThat(response.records[0].sourceSystemId).isEqualTo(person.crn)
      assertThat(response.records[0].firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(response.records[0].middleName).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(response.records[0].lastName).isEqualTo(person.getPrimaryName().lastName)
    }

    @Test
    fun `should return cluster record with multiple records details`() {
      val personKey = createPersonKey()
        .addPerson(createPerson(createRandomProbationPersonDetails()))
        .addPerson(createPerson(createRandomLibraPersonDetails()))
        .addPerson(createPerson(createRandomPrisonPersonDetails()))
        .addPerson(createPerson(createRandomCommonPlatformPersonDetails()))

      stubVisualiseCluster()

      val response = webTestClient.get()
        .uri(uuidClusterUrl(personKey.personUUID.toString()))
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(AdminClusterDetail::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response.uuid).isEqualTo(personKey.personUUID.toString())
      assertThat(response.status).isEqualTo("ACTIVE")
      assertThat(response.records.size).isEqualTo(4)
    }

    @Test
    fun `should return not found if cluster does not exist`() {
      webTestClient.get()
        .uri(uuidClusterUrl(UUID.randomUUID().toString()))
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.get()
        .uri(uuidClusterUrl(person.personKey?.personUUID.toString()))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      webTestClient.get()
        .uri(uuidClusterUrl(person.personKey?.personUUID.toString()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    private fun uuidClusterUrl(uuid: String) = ADMIN_UUID_CLUSTER_URL + uuid
  }

  @Nested
  inner class CRNClusterDetail {

    @Test
    fun `should return cluster record with one record details`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())

      stubVisualiseCluster()

      val response = webTestClient.get()
        .uri(crnClusterUrl(person.crn!!))
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(AdminClusterDetail::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
      assertThat(response.status).isEqualTo("ACTIVE")
      assertThat(response.records.size).isEqualTo(1)
      assertThat(response.records[0].sourceSystem).isEqualTo("DELIUS")
      assertThat(response.records[0].sourceSystemId).isEqualTo(person.crn)
      assertThat(response.records[0].firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(response.records[0].middleName).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(response.records[0].lastName).isEqualTo(person.getPrimaryName().lastName)
    }

    @Test
    fun `should return not found if cluster does not exist`() {
      webTestClient.get()
        .uri(crnClusterUrl(randomCrn()))
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.get()
        .uri(crnClusterUrl(person.crn!!))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())
      webTestClient.get()
        .uri(crnClusterUrl(person.crn!!))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    private fun crnClusterUrl(crn: String) = ADMIN_CRN_CLUSTER_URL + crn
  }

  @Nested
  inner class PrisonNumberClusterDetail {

    @Test
    fun `should return cluster record with one record details`() {
      val person = createPersonWithNewKey(createRandomPrisonPersonDetails())

      stubVisualiseCluster()

      val response = webTestClient.get()
        .uri(prisonNumberClusterUrl(person.prisonNumber!!))
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(AdminClusterDetail::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response.uuid).isEqualTo(person.personKey?.personUUID.toString())
      assertThat(response.status).isEqualTo("ACTIVE")
      assertThat(response.records.size).isEqualTo(1)
      assertThat(response.records[0].sourceSystem).isEqualTo("NOMIS")
      assertThat(response.records[0].sourceSystemId).isEqualTo(person.prisonNumber)
      assertThat(response.records[0].firstName).isEqualTo(person.getPrimaryName().firstName)
      assertThat(response.records[0].middleName).isEqualTo(person.getPrimaryName().middleNames)
      assertThat(response.records[0].lastName).isEqualTo(person.getPrimaryName().lastName)
    }

    @Test
    fun `should return not found if cluster does not exist`() {
      webTestClient.get()
        .uri(prisonNumberClusterUrl(randomPrisonNumber()))
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val person = createPersonWithNewKey(createRandomPrisonPersonDetails())
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.get()
        .uri(prisonNumberClusterUrl(person.prisonNumber!!))
        .authorised(listOf("UNSUPPORTED-ROLE"))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo(expectedErrorMessage)
    }

    @Test
    fun `should return UNAUTHORIZED 401 when role is not set`() {
      val person = createPersonWithNewKey(createRandomPrisonPersonDetails())
      webTestClient.get()
        .uri(prisonNumberClusterUrl(person.prisonNumber!!))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    private fun prisonNumberClusterUrl(prisonNumber: String) = ADMIN_PRISON_NUMBER_CLUSTER_URL + prisonNumber
  }

  private fun stubVisualiseCluster() = stubPostRequest(
    url = "/visualise-cluster",
    status = 200,
    responseBody = """ { "spec": {} } """.trimIndent(),
  )

  companion object {
    private const val ADMIN_UUID_CLUSTER_URL = "/admin/cluster/"
    private const val ADMIN_CRN_CLUSTER_URL = "/admin/cluster/probation/"
    private const val ADMIN_PRISON_NUMBER_CLUSTER_URL = "/admin/cluster/prison/"
  }
}
