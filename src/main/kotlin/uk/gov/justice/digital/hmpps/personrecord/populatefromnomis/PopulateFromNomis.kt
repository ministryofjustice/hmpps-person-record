package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

private const val OK = "OK"

@RestController
class PopulateFromNomis {

  @RequestMapping(method = [RequestMethod.POST], value = ["/populatefromnomis"])
  fun populate(): String {
    return OK
  }
}
