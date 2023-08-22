package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.web.reactive.function.client.WebClient

class AuthenticatingRestClient(
  private val webClient: WebClient,
  private val oauthClient: String,
  private val authEnabled: Boolean,
) {
  fun get(path: String): WebClient.RequestHeadersSpec<*> {
    val request = webClient
      .get()
      .uri(path)
      .accept(MediaType.APPLICATION_JSON)
    return if (authEnabled) {
      request.attributes(clientRegistrationId(oauthClient))
    } else {
      request
    }
  }

  fun post(path: String, body: Any): WebClient.RequestHeadersSpec<*> {
    val request = webClient
      .post()
      .uri(path)
      .accept(MediaType.APPLICATION_JSON)
    val authed = if (authEnabled) {
      request.attributes(clientRegistrationId(oauthClient))
    } else {
      request
    }
    return authed.bodyValue(body)
  }

  fun put(path: String, body: Any): WebClient.RequestHeadersSpec<*> {
    val request = webClient
      .put()
      .uri(path)
      .accept(MediaType.APPLICATION_JSON)
    val authed = if (authEnabled) {
      request.attributes(clientRegistrationId(oauthClient))
    } else {
      request
    }
    return authed.bodyValue(body)
  }
}
