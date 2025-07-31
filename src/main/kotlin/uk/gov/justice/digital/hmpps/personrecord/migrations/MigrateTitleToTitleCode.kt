package uk.gov.justice.digital.hmpps.personrecord.migrations

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class MigrateTitleToTitleCode {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/migrate/title-to-title-code"])
  suspend fun migrate(): String {

    return OK
  }

  suspend fun collectAndReportStats() {
    CoroutineScope(Dispatchers.Default).launch {

    }
  }

  companion object {
    private const val OK = "OK"
  }
}