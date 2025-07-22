package uk.gov.justice.digital.hmpps.personrecord.controller.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.admin.PaginatedResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.MERGED
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.RECLUSTER_MERGE
import java.util.UUID

class ClustersApiIntTest : WebTestBase() {

  @Nested
  inner class ClusterApi {

    @Test
    fun `should return cluster record with one record details`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails())

      val response = webTestClient.get()
        .uri(clusterUrl(person.personKey?.personUUID.toString()))
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
    fun `should return cluster record with multiple records details`() {
      val personKey = createPersonKey()
        .addPerson(createPerson(createRandomProbationPersonDetails()))
        .addPerson(createPerson(createRandomLibraPersonDetails()))
        .addPerson(createPerson(createRandomPrisonPersonDetails()))
        .addPerson(createPerson(createRandomCommonPlatformPersonDetails()))

      val response = webTestClient.get()
        .uri(clusterUrl(personKey.personUUID.toString()))
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
        .uri(clusterUrl(UUID.randomUUID().toString()))
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
        .uri(clusterUrl(person.personKey?.personUUID.toString()))
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
        .uri(clusterUrl(person.personKey?.personUUID.toString()))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    private fun clusterUrl(uuid: String) = ADMIN_CLUSTER_URL + uuid
  }

  @Nested
  inner class ClustersApi {

    @BeforeEach
    fun beforeEach() {
      personRepository.deleteAll()
      personKeyRepository.deleteAll()
    }

    @Test
    fun `should return list of clusters with composition`() {
      val person = createPersonWithNewKey(createRandomProbationPersonDetails(), status = UUIDStatusType.NEEDS_ATTENTION)

      val responseType = object : ParameterizedTypeReference<PaginatedResponse<AdminCluster>>() {}
      val response = webTestClient.get()
        .uri(ADMIN_CLUSTERS_URL)
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(responseType)
        .returnResult()
        .responseBody!!

      assertThat(response.content.size).isEqualTo(1)
      assertThat(response.pagination.count).isEqualTo(1)
      assertThat(response.pagination.totalPages).isEqualTo(1)
      assertThat(response.pagination.page).isEqualTo(1)
      assertThat(response.pagination.perPage).isEqualTo(20)

      assertThat(response.content[0].uuid).isEqualTo(person.personKey?.personUUID.toString())
      assertThat(response.content[0].recordComposition[0].count).isEqualTo(0)
      assertThat(response.content[0].recordComposition[0].sourceSystem).isEqualTo(COMMON_PLATFORM)
      assertThat(response.content[0].recordComposition[1].count).isEqualTo(1)
      assertThat(response.content[0].recordComposition[1].sourceSystem).isEqualTo(DELIUS)
      assertThat(response.content[0].recordComposition[2].count).isEqualTo(0)
      assertThat(response.content[0].recordComposition[2].sourceSystem).isEqualTo(LIBRA)
      assertThat(response.content[0].recordComposition[3].count).isEqualTo(0)
      assertThat(response.content[0].recordComposition[3].sourceSystem).isEqualTo(NOMIS)
    }

    @Test
    fun `should not return clusters that are not NEEDS ATTENTION`() {
      createPersonWithNewKey(createRandomProbationPersonDetails(), status = ACTIVE)
      createPersonWithNewKey(createRandomProbationPersonDetails(), status = MERGED)
      createPersonWithNewKey(createRandomProbationPersonDetails(), status = RECLUSTER_MERGE)

      val responseType = object : ParameterizedTypeReference<PaginatedResponse<AdminCluster>>() {}
      val response = webTestClient.get()
        .uri(ADMIN_CLUSTERS_URL)
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(responseType)
        .returnResult()
        .responseBody!!

      assertThat(response.content.size).isEqualTo(0)
      assertThat(response.pagination.count).isEqualTo(0)
      assertThat(response.pagination.totalPages).isEqualTo(0)
      assertThat(response.pagination.page).isEqualTo(1)
    }

    @Test
    fun `should return list of multiple clusters with composition`() {
      val cluster1 = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
        .addPerson(createPerson(createRandomProbationPersonDetails()))
        .addPerson(createPerson(createRandomPrisonPersonDetails()))
        .addPerson(createPerson(createRandomCommonPlatformPersonDetails()))
        .addPerson(createPerson(createRandomLibraPersonDetails()))

      val cluster2 = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION)
        .addPerson(createPerson(createRandomProbationPersonDetails()))
        .addPerson(createPerson(createRandomProbationPersonDetails()))

      val responseType = object : ParameterizedTypeReference<PaginatedResponse<AdminCluster>>() {}
      val response = webTestClient.get()
        .uri(ADMIN_CLUSTERS_URL)
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(responseType)
        .returnResult()
        .responseBody!!

      assertThat(response.content.size).isEqualTo(2)
      assertThat(response.pagination.count).isEqualTo(2)
      assertThat(response.pagination.totalPages).isEqualTo(1)
      assertThat(response.pagination.page).isEqualTo(1)
      assertThat(response.pagination.perPage).isEqualTo(20)

      assertThat(response.content[0].uuid).isEqualTo(cluster1.personUUID.toString())
      assertThat(response.content[0].recordComposition[0].count).isEqualTo(1)
      assertThat(response.content[0].recordComposition[0].sourceSystem).isEqualTo(COMMON_PLATFORM)
      assertThat(response.content[0].recordComposition[1].count).isEqualTo(1)
      assertThat(response.content[0].recordComposition[1].sourceSystem).isEqualTo(DELIUS)
      assertThat(response.content[0].recordComposition[2].count).isEqualTo(1)
      assertThat(response.content[0].recordComposition[2].sourceSystem).isEqualTo(LIBRA)
      assertThat(response.content[0].recordComposition[3].count).isEqualTo(1)
      assertThat(response.content[0].recordComposition[3].sourceSystem).isEqualTo(NOMIS)

      assertThat(response.content[1].uuid).isEqualTo(cluster2.personUUID.toString())
      assertThat(response.content[1].recordComposition.first { it.sourceSystem == COMMON_PLATFORM }.count).isEqualTo(0)
      assertThat(response.content[1].recordComposition[1].count).isEqualTo(2)
      assertThat(response.content[1].recordComposition[1].sourceSystem).isEqualTo(DELIUS)
      assertThat(response.content[1].recordComposition[2].count).isEqualTo(0)
      assertThat(response.content[1].recordComposition[2].sourceSystem).isEqualTo(LIBRA)
      assertThat(response.content[1].recordComposition[3].count).isEqualTo(0)
      assertThat(response.content[1].recordComposition[3].sourceSystem).isEqualTo(NOMIS)
    }

    @Test
    fun `should return specified page`() {
      repeat(20) {
        createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION).addPerson(createPerson(createRandomProbationPersonDetails()))
      }

      val nextPageCluster = createPersonKey(status = UUIDStatusType.NEEDS_ATTENTION).addPerson(createPerson(createRandomProbationPersonDetails()))

      val responseType = object : ParameterizedTypeReference<PaginatedResponse<AdminCluster>>() {}
      val response = webTestClient.get()
        .uri("$ADMIN_CLUSTERS_URL?page=2")
        .authorised(roles = listOf(Roles.PERSON_RECORD_ADMIN_READ_ONLY))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(responseType)
        .returnResult()
        .responseBody!!

      assertThat(response.content.size).isEqualTo(1)
      assertThat(response.pagination.count).isEqualTo(1)
      assertThat(response.pagination.totalPages).isEqualTo(2)
      assertThat(response.pagination.page).isEqualTo(2)
      assertThat(response.pagination.perPage).isEqualTo(20)

      assertThat(response.content[0].uuid).isEqualTo(nextPageCluster.personUUID.toString())
    }

    @Test
    fun `should return Access Denied 403 when role is wrong`() {
      val expectedErrorMessage = "Forbidden: Access Denied"
      webTestClient.get()
        .uri(ADMIN_CLUSTERS_URL)
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
      webTestClient.get()
        .uri(ADMIN_CLUSTERS_URL)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  companion object {
    private const val ADMIN_CLUSTERS_URL = "/admin/clusters"
    private const val ADMIN_CLUSTER_URL = "/admin/cluster/"
  }
}
