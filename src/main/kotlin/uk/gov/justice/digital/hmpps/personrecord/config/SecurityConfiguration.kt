package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
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
}
