package uk.gov.justice.digital.hmpps.personrecord.populatefromnomis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

private const val OK = "OK"

@RestController
class PopulateFromNomis {

  @RequestMapping(method = [RequestMethod.POST], value = ["/populatefromnomis"])
  suspend fun populate(): String {
    populatePages()
    println("returning now")
    return OK
  }

  suspend fun populatePages() {
    CoroutineScope(Job()).launch {
      (1..5).forEach {
        println("this is loop $it")
        delay(1000L)
      }
    }
  }
}
