package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
class WebClientConfig(
  @Value("\${person-match.base-url}") val personMatchUrl: String,
  @Value("\${prisoner-search.base-url}") val prisonerSearchUrl: String,
  @Value("\${core-person-record-and-delius.base-url}") val corePersonRecordAndDeliusUrl: String,
  @Value("\${retry.timeout}") val timeout: Long,
) {

  @Bean
  fun personMatchWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "person-match",
    url = personMatchUrl,
    timeout = Duration.ofMillis(timeout),
  )

  @Bean
  fun prisonerSearchWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "prisoner-search",
    url = prisonerSearchUrl,
    timeout = Duration.ofMillis(timeout),
  )

  @Bean
  fun corePersonRecordAndDeliusWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "core-person-record-and-delius",
    url = corePersonRecordAndDeliusUrl,
    timeout = Duration.ofMillis(timeout),
  )
}
