package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * Kept out of the @SpringBootApplication class so that test slices (e.g. @WebMvcTest, @DataJpaTest)
 * which use HmppsPersonRecord as their configuration source don't unnecessarily pull in the JPA
 * auditing infrastructure and fail with "JPA metamodel must not be empty" when no @Entity classes
 * are on the test's component-scan path.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
class JpaAuditingConfiguration
