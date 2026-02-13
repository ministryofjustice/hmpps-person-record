package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
class SecurityConfiguration {

  @Bean
  fun pageableCustomizer(): PageableHandlerMethodArgumentResolverCustomizer = PageableHandlerMethodArgumentResolverCustomizer { resolver: PageableHandlerMethodArgumentResolver ->
    resolver.setOneIndexedParameters(true)
  }

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    unauthorizedRequestPaths {
      addPaths = setOf(
        "/queue-admin/retry-all-dlqs",
        "/jobs/recordcountreport",
        "/jobs/recluster-needs-attention",
        "/jobs/service-now/generate-delius-merge-requests",
        "/admin/recluster",
        "/admin/exclusion/prisoner/**",
      )
    }
  }
}
