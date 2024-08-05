package uk.gov.justice.digital.hmpps.personrecord.seeding

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController


@RestController
class GenerateTermFrequencies() {

  @RequestMapping(method = [RequestMethod.POST], value = ["/generate/termfrequencies"])
  suspend fun populate(): String {
    return OK
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val OK = "OK"
  }
}
