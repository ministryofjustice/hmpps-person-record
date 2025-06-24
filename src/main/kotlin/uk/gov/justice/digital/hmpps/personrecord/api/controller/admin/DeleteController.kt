package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DeleteController(
) {

  @Hidden
  @PostMapping("/admin/delete")
  suspend fun postDelete(
//    @RequestBody records: List<AdminDeleteRecord>,
  ) {
    CoroutineScope(Dispatchers.Default).launch {
    }
  }

  companion object {
//    private val log = LoggerFactory.getLogger(this::class.java)
  }
}