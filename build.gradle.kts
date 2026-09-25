@file:Suppress("UnstableApiUsage")

kotlin {
  jvmToolchain(25)
}
plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.10"
  kotlin("plugin.spring") version "2.4.10"
  kotlin("jvm") version "2.4.10"
  kotlin("plugin.jpa") version "2.4.10"
  id("org.jetbrains.kotlinx.kover") version "0.9.9"
  id("org.owasp.dependencycheck") version "12.2.2"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.2")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.1")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("jakarta.validation:jakarta.validation-api:3.1.1")

  implementation("software.amazon.sns:sns-extended-client:2.1.0")
  implementation("com.jayway.jsonpath:json-path:3.0.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

  runtimeOnly("org.postgresql:postgresql:42.7.13")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.wiremock:wiremock-standalone:3.13.2")

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.jmock:jmock:2.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.0.2")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")

  testImplementation("au.com.dius.pact.provider:junit5spring:4.7.5")
}

repositories {
  mavenCentral()
}

val test = testing.suites.named<JvmTestSuite>(JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)

tasks.register<Test>("initialiseDatabase") {
  description = "A simple task which starts the Spring ApplicationContext and therefore runs flyway migrations"
  testClassesDirs = files(test.map { it.sources.output.classesDirs })
  classpath = files(test.map { it.sources.runtimeClasspath })
  include("**/InitialiseDatabase.class")
  onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
}

tasks.register<Test>("e2eTest") {
  description = "runs end to end tests against docker"
  testClassesDirs = files(test.map { it.sources.output.classesDirs })
  classpath = files(test.map { it.sources.runtimeClasspath })
  include("**/**E2ETest.class")
  onlyIf { gradle.startParameter.taskNames.contains("e2eTest") }
}

tasks.register<Exec>("setUpS3Bucket") {
  description = "creates S3 buckets in localstack for testing"
  executable = "sh"
  args = listOf("-c", "./src/test/resources/localstack/setup-aws.sh")
}

fun registerPactVerificationTask(
  name: String,
  description: String,
  testClass: String,
  consumer: String,
) = tasks.register<Test>(name) {
  enabled = false
  this.description = description
  testClassesDirs = files(test.map { it.sources.output.classesDirs })
  classpath = files(test.map { it.sources.runtimeClasspath })
  filter.includeTestsMatching(testClass)
  group = "verification"

  systemProperty(
    "pactbroker.url",
    System.getProperty("pactbroker.url")
      ?: System.getenv("PACT_BROKER_URL")
      ?: "https://pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk",
  )
  systemProperty("pactbroker.auth.username", System.getenv("PACT_BROKER_USERNAME") ?: "")
  systemProperty("pactbroker.auth.password", System.getenv("PACT_BROKER_PASSWORD") ?: "")

  val consumerBranch = System.getenv("PACT_CONSUMER_BRANCH")?.takeIf { it.isNotBlank() }
  val selectors = if (consumerBranch != null) {
    """[{"consumer":"$consumer","branch":"$consumerBranch"}]"""
  } else {
    """[{"consumer":"$consumer","mainBranch":true},{"consumer":"$consumer","deployed":true}]"""
  }
  systemProperty("pactbroker.consumerversionselectors.rawjson", selectors)

  systemProperty("pact.provider.version", System.getenv("PACT_PROVIDER_VERSION") ?: "local")
  systemProperty("pact.provider.branch", System.getenv("GITHUB_BRANCH") ?: "local")
  systemProperty("pactbroker.providerBranch", System.getenv("GITHUB_BRANCH") ?: "local")
  systemProperty("pact.verifier.publishResults", System.getenv("PACT_PUBLISH_RESULTS") ?: "false")
}

val pactProbationTest = registerPactVerificationTask(
  name = "pactProbationTest",
  description = "Run Pact provider verification for probation contracts",
  testClass = "uk.gov.justice.digital.hmpps.personrecord.pacttest.ProbationProviderPactTest",
  consumer = "probation-test-consumer",
).apply {
  configure { enabled = true }
}

val pactCourtDataIngestionTest = registerPactVerificationTask(
  name = "pactCourtDataIngestionTest",
  description = "Run Pact provider verification for court data ingestion contracts",
  testClass = "uk.gov.justice.digital.hmpps.personrecord.pacttest.CourtDataIngestionProviderPactTest",
  consumer = "hmpps-court-data-ingestion-api",
).apply {
  configure { enabled = true }
}

tasks.register("pactTest") {
  enabled = true
  description = "Run and publish all Pact provider tests"
  group = "verification"
  dependsOn(pactProbationTest, pactCourtDataIngestionTest)
}

tasks {
  test {
    exclude("**/InitialiseDatabase.class")
    exclude("**/**E2ETest.class")
    exclude("**/pacttest/**")
  }

  getByName("check") {
    dependsOn(":ktlintCheck", "setUpS3Bucket")
  }

  getByName("koverHtmlReport") {
    dependsOn("check")
  }

  withType<JavaCompile>().configureEach {
    options.isFork = true
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}
