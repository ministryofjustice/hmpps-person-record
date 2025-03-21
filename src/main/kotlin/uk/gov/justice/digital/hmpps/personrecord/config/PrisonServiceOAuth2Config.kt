package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager

@Configuration
class PrisonServiceOAuth2Config(authorizedClientManager: OAuth2AuthorizedClientManager) : FeignConfig(authorizedClientManager) {

  override fun registrationId() = "prison-service"
}
