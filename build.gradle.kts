kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.0"
  kotlin("plugin.spring") version "2.1.21"
  kotlin("jvm") version "2.1.21"
  kotlin("plugin.jpa") version "2.1.21"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.4.7")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.6")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("jakarta.validation:jakarta.validation-api:3.1.1")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")

  implementation("software.amazon.sns:sns-extended-client:2.1.0")
  implementation("com.jayway.jsonpath:json-path:2.9.0")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.34")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.1")

  runtimeOnly("org.postgresql:postgresql:42.7.7")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.wiremock:wiremock-standalone:3.13.1")

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.jmock:jmock:2.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.4.7")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
  mavenCentral()
}

detekt {
  source.setFrom("$projectDir/src/main")
  buildUponDefaultConfig = true // preconfigure defaults
  allRules = false // activate all available (even unstable) rules.
  config.setFrom("$projectDir/detekt.yml") // point to your custom config defining rules
}

tasks {
  register("initialiseDatabase", Test::class) {
    include("**/InitialiseDatabase.class")
  }

  register("integrationTest", Test::class) {
    include("**/IntegrationTest.class")
  }

  test {
    exclude("**/InitialiseDatabase.class")
    exclude("**/IntegrationTest.class")
  }

  getByName("initialiseDatabase") {
    onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
  }

  getByName("integrationTest") {
    onlyIf { gradle.startParameter.taskNames.contains("integrationTest") }
  }

  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
    finalizedBy("koverHtmlReport")
  }

  withType<JavaCompile>().configureEach {
    options.isFork = true
  }
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
    }
  }
}
