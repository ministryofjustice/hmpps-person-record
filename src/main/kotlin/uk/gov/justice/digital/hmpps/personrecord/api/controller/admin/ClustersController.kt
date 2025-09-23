package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.SourceSystemComposition
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import java.util.UUID

@RestController
class ClustersController(
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
) {

  @Hidden
  @PreAuthorize("hasRole('${Roles.PERSON_RECORD_ADMIN_READ_ONLY}')")
  @GetMapping("/admin/cluster/{uuid}")
  suspend fun getCluster(
    @PathVariable(name = "uuid") uuid: UUID,
  ): AdminClusterDetail {
    val personKeyEntity = withContext(Dispatchers.IO) {
      personKeyRepository.findByPersonUUID(uuid)
    } ?: throw ResourceNotFoundException(uuid.toString())
    val clusterVisualisationSpec = personMatchService.retrieveClusterVisualisationSpec(personKeyEntity).spec
    println(clusterVisualisationSpec)
    return AdminClusterDetail.from(personKeyEntity, clusterVisualisationSpec)
  }

  @Hidden
  @PreAuthorize("hasRole('${Roles.PERSON_RECORD_ADMIN_READ_ONLY}')")
  @GetMapping("/admin/clusters")
  suspend fun getClusters(
    @RequestParam(defaultValue = "1") page: Int,
  ): PaginatedResponse<AdminCluster> {
    val evaluatedPageable: Pageable = Pageable.ofSize(DEFAULT_PAGE_SIZE).withPage(page - 1)
    val paginatedClusters = withContext(Dispatchers.IO) {
      personKeyRepository.findAllByStatusOrderById(UUIDStatusType.NEEDS_ATTENTION, evaluatedPageable)
    }
    val clusters = paginatedClusters.content.map {
      AdminCluster(
        uuid = it.personUUID.toString(),
        recordComposition = listOf(
          SourceSystemComposition(COMMON_PLATFORM, it.personEntities.getRecordCountBySourceSystem(COMMON_PLATFORM)),
          SourceSystemComposition(DELIUS, it.personEntities.getRecordCountBySourceSystem(DELIUS)),
          SourceSystemComposition(LIBRA, it.personEntities.getRecordCountBySourceSystem(LIBRA)),
          SourceSystemComposition(NOMIS, it.personEntities.getRecordCountBySourceSystem(NOMIS)),
        ),
      )
    }
    return PaginatedResponse(
      content = clusters,
      pagination = Pagination(
        isLastPage = paginatedClusters.isLast,
        count = paginatedClusters.numberOfElements,
        page = paginatedClusters.number + 1,
        perPage = paginatedClusters.size,
        totalCount = paginatedClusters.totalElements,
        totalPages = paginatedClusters.totalPages,
      ),
    )
  }

  private fun List<PersonEntity>.getRecordCountBySourceSystem(sourceSystemType: SourceSystemType): Int = this.filter { record -> record.sourceSystem == sourceSystemType }.size

  companion object {
    private const val DEFAULT_PAGE_SIZE = 20
  }
}

class PaginatedResponse<T>(
  val content: List<T>,
  val pagination: Pagination,
)

data class Pagination(
  @Schema(description = "Is the current page the last one?", example = "true")
  val isLastPage: Boolean,
  @Schema(description = "The number of results in `data` for the current page", example = "1")
  val count: Int,
  @Schema(description = "The current page number", example = "1")
  val page: Int,
  @Schema(description = "The maximum number of results in `data` for a page", example = "10")
  val perPage: Int,
  @Schema(description = "The total number of results in `data` across all pages", example = "1")
  val totalCount: Long,
  @Schema(description = "The total number of pages", example = "1")
  val totalPages: Int,
)
