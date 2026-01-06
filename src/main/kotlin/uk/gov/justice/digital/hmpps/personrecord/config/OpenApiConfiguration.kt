package uk.gov.justice.digital.hmpps.personrecord.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String? = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://hmpps-person-record-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://hmpps-person-record-preprod.hmpps.service.justice.gov.uk").description("Pre-Production"),
        Server().url("https://hmpps-person-record.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .tags(
      listOf(
        Tag().name("Search").description("APIs for person search"),
      ),
    )
    .info(
      Info().title("HMPPS Core Person Record")
        .version(version)
        .description(this.javaClass.getResource("/documentation/service-description.html")!!.readText())
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk"))
        .contact(Contact().name("HMPPS Person Record").email("hmpps-person-record@digital.justice.gov.uk")),
    )
    .components(
      Components().addSecuritySchemes(
        "api-role",
        SecurityScheme().addBearerJwtRequirement(Roles.API_READ_ONLY),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read")))

  @Bean
  fun hiddenApi(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("hidden")
    .pathsToExclude("/queue-admin/**")
    .build()
}

private fun SecurityScheme.addBearerJwtRequirement(role: String): SecurityScheme = type(SecurityScheme.Type.HTTP)
  .scheme("bearer")
  .bearerFormat("JWT")
  .`in`(SecurityScheme.In.HEADER)
  .name("Authorization")
  .description("A HMPPS Auth access token with the `$role` role.")
