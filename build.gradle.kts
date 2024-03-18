plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.3"
  kotlin("plugin.spring") version "1.9.23"
  kotlin("jvm") version "1.9.23"
  kotlin("plugin.jpa") version "1.9.23"
  id("io.gitlab.arturbosch.detekt") version "1.23.5"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
    }
  }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("jakarta.validation:jakarta.validation-api:3.0.2")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.0")
  implementation("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.1")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.2.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

  runtimeOnly("org.postgresql:postgresql:42.7.3")
  runtimeOnly("org.flywaydb:flyway-core:10.10.0")
  runtimeOnly("org.flywaydb:flyway-database-postgresql:10.10.0")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.testcontainers:junit-jupiter:1.19.7")
  testImplementation("org.testcontainers:postgresql:1.19.7")
  testImplementation("org.testcontainers:localstack:1.19.7")
  testImplementation("org.wiremock:wiremock-standalone:3.4.2")
  testImplementation("io.jsonwebtoken:jjwt-api:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.23")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("org.jmock:jmock:2.13.1")
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
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
  }
}
