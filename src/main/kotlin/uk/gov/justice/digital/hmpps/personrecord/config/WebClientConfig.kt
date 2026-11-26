package uk.gov.justice.digital.hmpps.personrecord.config

import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
class WebClientConfig(
  @Value($$"${person-match.base-url}") val personMatchUrl: String,
  @Value($$"${prisoner-search.base-url}") val prisonerSearchUrl: String,
  @Value($$"${core-person-record-and-delius.base-url}") val corePersonRecordAndDeliusUrl: String,
  @Value($$"${sas.base-url}") val sasUrl: String,
  @Value($$"${service-now.base-url}") val serviceNowUrl: String,
  @Value($$"${prison-api.base-url}") val prisonApiUrl: String,
  @Value($$"${retry.timeout}") val timeout: Long,
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

  @Bean
  fun sasWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "sas",
    url = sasUrl,
    timeout = Duration.ofMillis(timeout),
  )

  @Bean
  fun serviceNowWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "service-now",
    url = serviceNowUrl,
    timeout = Duration.ofMillis(timeout),
  )

  @Bean
  fun prisonWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient {
    val provider = ConnectionProvider.builder("prison-api-pool")
      .maxConnections(500)
      .pendingAcquireMaxCount(500 * 2)
      .pendingAcquireTimeout(Duration.ofSeconds(5))
      .maxIdleTime(Duration.ofSeconds(20))
      .maxLifeTime(Duration.ofMinutes(5))
      .evictInBackground(Duration.ofSeconds(30))
      .build()

    val httpClient = HttpClient.create(provider)
      .proxyWithSystemProperties()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
      .responseTimeout(Duration.ofMillis(timeout))

    return builder.authorisedWebClient(
      authorizedClientManager = authorizedClientManager,
      registrationId = "prison-api",
      url = prisonApiUrl,
      timeout = Duration.ofMillis(timeout),
    ).mutate()
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .build()
  }
}
