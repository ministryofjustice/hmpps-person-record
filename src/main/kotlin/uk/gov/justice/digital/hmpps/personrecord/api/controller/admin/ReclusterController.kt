package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminReclusterRecord

@RestController
class ReclusterController(private val adminReclusterService: AdminReclusterService) {

  @Hidden
  @PostMapping("/admin/recluster")
  suspend fun postRecluster(
    @RequestBody adminReclusterRecords: List<AdminReclusterRecord>,
  ) {
    CoroutineScope(Dispatchers.Default).launch {
      adminReclusterService.recluster(adminReclusterRecords)
    }
  }
}
