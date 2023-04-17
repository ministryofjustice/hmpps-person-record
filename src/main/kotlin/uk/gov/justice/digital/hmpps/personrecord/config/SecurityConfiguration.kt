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
      .csrf().disable()
      .authorizeHttpRequests { authorize ->
        authorize
          .requestMatchers(
            "/health/**",
            "/ping",
            "/swagger-ui.html",
            "/swagger-ui/**",
          ).permitAll()
          .anyRequest().hasAuthority("ROLE_VIEW_PRISONER_DATA")
      }
      .oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())

    return http.build()
  }
}
