package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("seeding")
class PopulateClusters {

  @Hidden
  @RequestMapping(method = [POST], value = ["/populateclusters"])
  suspend fun populate(): String {
    populateClusters()
    return "OK"
  }

  suspend fun populateClusters() {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("Cluster population finished")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
