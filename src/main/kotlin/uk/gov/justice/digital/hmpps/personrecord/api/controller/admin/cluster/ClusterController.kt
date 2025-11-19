package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin.cluster

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import java.util.UUID

@RestController
class ClusterController(
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
  private val personRepository: PersonRepository,
) {

  @Hidden
  @PreAuthorize("hasRole('${Roles.PERSON_RECORD_ADMIN_READ_ONLY}')")
  @GetMapping("/admin/cluster/{uuid}")
  suspend fun getClusterFromUUID(
    @PathVariable(name = "uuid") uuid: UUID,
  ): AdminClusterDetail {
    val personKeyEntity = withContext(Dispatchers.IO) {
      personKeyRepository.findByPersonUUID(uuid)
    } ?: throw ResourceNotFoundException(uuid.toString())
    return getClusterDetail(personKeyEntity)
  }

  @Hidden
  @PreAuthorize("hasRole('${Roles.PERSON_RECORD_ADMIN_READ_ONLY}')")
  @GetMapping("/admin/cluster/probation/{crn}")
  suspend fun getClusterFromCRN(
    @PathVariable(name = "crn") crn: String,
  ): AdminClusterDetail {
    val personKeyEntity = withContext(Dispatchers.IO) {
      personRepository.findByCrn(crn)?.personKey
    } ?: throw ResourceNotFoundException(crn)
    return getClusterDetail(personKeyEntity)
  }

  @Hidden
  @PreAuthorize("hasRole('${Roles.PERSON_RECORD_ADMIN_READ_ONLY}')")
  @GetMapping("/admin/cluster/prison/{prisonNumber}")
  suspend fun getClusterFromPrisonNumber(
    @PathVariable(name = "prisonNumber") prisonNumber: String,
  ): AdminClusterDetail {
    val personKeyEntity = withContext(Dispatchers.IO) {
      personRepository.findByPrisonNumber(prisonNumber)?.personKey
    } ?: throw ResourceNotFoundException(prisonNumber)
    return getClusterDetail(personKeyEntity)
  }

  private fun getClusterDetail(personKeyEntity: PersonKeyEntity): AdminClusterDetail {
    val clusterVisualisationSpec = personMatchService.retrieveClusterVisualisationSpec(personKeyEntity).spec
    return AdminClusterDetail.from(personKeyEntity, clusterVisualisationSpec)
  }
}
