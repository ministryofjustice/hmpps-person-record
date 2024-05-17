package uk.gov.justice.digital.hmpps.personrecord.integration

import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTestBase {
  companion object {

    @JvmStatic
    @RegisterExtension
    var wireMockExtension: WireMockExtension = WireMockExtension.newInstance()
      .options(wireMockConfig().port(8090))
      .failOnUnmatchedRequests(true)
      .build()
  }

  @Autowired
  lateinit var personRepository: PersonRepository

  @BeforeEach
  fun beforeEach() {
    personRepository.deleteAll()
  }
}
