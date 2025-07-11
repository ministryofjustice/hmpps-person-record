package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.SourceSystemComposition
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
    @PageableDefault(size = DEFAULT_PAGE_SIZE) pageable: Pageable,
  ): Page<AdminCluster> {
    val evaluatedPageable: Pageable = pageable.check()
    return withContext(Dispatchers.IO) {
      personKeyRepository.findAllByStatus(UUIDStatusType.NEEDS_ATTENTION, evaluatedPageable)
    }.map {
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
  }

  private fun Pageable.check() = when {
    this.pageSize > MAX_PAGE_SIZE -> Pageable.ofSize(DEFAULT_PAGE_SIZE).withPage(this.pageNumber)
    else -> this
  }

  private fun List<PersonEntity>.getRecordCountBySourceSystem(sourceSystemType: SourceSystemType): Int = this.filter { record -> record.sourceSystem == sourceSystemType }.size

  companion object {
    private const val DEFAULT_PAGE_SIZE = 20
    private const val MAX_PAGE_SIZE = 50
  }
}
