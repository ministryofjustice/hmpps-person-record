package uk.gov.justice.digital.hmpps.personrecord.config

import feign.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.core.OAuth2AuthenticationException

abstract class FeignConfig(
  private val authorizedClientManager: OAuth2AuthorizedClientManager,
) {

  abstract fun registrationId(): String

  @Bean
  open fun requestInterceptor() = RequestInterceptor { template ->
    template.header(HttpHeaders.AUTHORIZATION, "Bearer ${getAccessToken()}")
  }

  private fun getAccessToken(): String {
    val authentication = SecurityContextHolder.getContext().authentication ?: AnonymousAuthenticationToken(
      "hmpps-auth",
      "anonymous",
      AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
    )
    val request = OAuth2AuthorizeRequest
      .withClientRegistrationId(registrationId())
      .principal(authentication)
      .build()

    val authorizedClient = authorizedClientManager.authorize(request)
    val accessToken = authorizedClient?.accessToken

    return accessToken?.tokenValue
      ?: throw OAuth2AuthenticationException("Unable to retrieve access token")
  }
}
