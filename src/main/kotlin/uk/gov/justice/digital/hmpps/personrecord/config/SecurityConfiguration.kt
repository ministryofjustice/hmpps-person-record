package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

  @Bean
  @Throws(java.lang.Exception::class)
  fun filterChain(http: HttpSecurity): SecurityFilterChain? {
    http
      .csrf { csrf -> csrf.disable() }
      .authorizeHttpRequests { authorize ->
        authorize
          .requestMatchers(
            "/health/**",
            "/ping",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
          ).permitAll()
//          .anyRequest().hasAuthority("ROLE_VIEW_PRISONER_DATA") TODO ensure appropriate role is added back in at some point
          .anyRequest().permitAll() // TODO delete this line when role is added in above
      }
      .oauth2ResourceServer { oauth2ResourceServer ->
        oauth2ResourceServer
          .jwt { jwt ->
            jwt.jwtAuthenticationConverter(AuthAwareTokenConverter())
          }
      }

    return http.build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    authorizedClientRepository: OAuth2AuthorizedClientRepository?,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build()

    val authorizedClientManager = DefaultOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      authorizedClientRepository,
    )

    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)

    return authorizedClientManager
  }
}
