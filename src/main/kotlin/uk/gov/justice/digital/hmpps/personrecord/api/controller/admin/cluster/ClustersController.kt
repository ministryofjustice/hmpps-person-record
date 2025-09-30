package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin.cluster

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.SourceSystemComposition
import uk.gov.justice.digital.hmpps.personrecord.extensions.sort
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType

@RestController
class ClustersController(
  private val personKeyRepository: PersonKeyRepository,
) {

  @Hidden
  @PreAuthorize("hasRole('${Roles.PERSON_RECORD_ADMIN_READ_ONLY}')")
  @GetMapping("/admin/clusters")
  suspend fun getClusters(
    @RequestParam(defaultValue = "1") page: Int,
  ): PaginatedResponse {
    val paginatedClusters = personKeyRepository.findAllByStatus(UUIDStatusType.NEEDS_ATTENTION)

    val clusters = paginatedClusters.map {
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

    return paginate(clusters.sort(), page)
  }

  private fun paginate(
    clusters: List<AdminCluster>,
    page: Int,
  ): PaginatedResponse {
    val totalFoundClusters = clusters.size.toLong()
    val currentIndex = (page - 1) * DEFAULT_PAGE_SIZE
    val isLast = (totalFoundClusters - currentIndex) <= DEFAULT_PAGE_SIZE
    val totalPages = when {
      totalFoundClusters > 0 -> (totalFoundClusters / DEFAULT_PAGE_SIZE) + 1
      else -> 0
    }.toInt()

    val lastIndex = when {
      isLast -> (totalFoundClusters % DEFAULT_PAGE_SIZE) + currentIndex
      else -> currentIndex + DEFAULT_PAGE_SIZE
    }.toInt()

    val content = clusters.subList(currentIndex, lastIndex)
    return PaginatedResponse(
      content = content,
      pagination = Pagination(
        isLastPage = isLast,
        totalPages = totalPages,
      ),
    )
  }

  private fun List<PersonEntity>.getRecordCountBySourceSystem(sourceSystemType: SourceSystemType): Int = this.filter { record -> record.sourceSystem == sourceSystemType }.size

  companion object {
    private const val DEFAULT_PAGE_SIZE = 20
  }
}

class PaginatedResponse(
  val content: List<AdminCluster>,
  val pagination: Pagination,
)

data class Pagination(
  @Schema(description = "Is the current page the last one?", example = "true")
  val isLastPage: Boolean,
  @Schema(description = "The total number of pages", example = "1")
  val totalPages: Int,
)
