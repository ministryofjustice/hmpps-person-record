kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.0"
  kotlin("plugin.spring") version "2.0.0"
  kotlin("jvm") version "2.0.0"
  kotlin("plugin.jpa") version "2.0.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.6"
  id("org.jetbrains.kotlinx.kover") version "0.8.1"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("jakarta.validation:jakarta.validation-api:3.1.0")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.22")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.2")
  constraints {
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1") {
      because("1.77 has CVEs")
    }
  }
  implementation("org.springframework.cloud:spring-cloud-dependencies:2023.0.2")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:4.0.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.5.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

  runtimeOnly("org.postgresql:postgresql:42.7.3")
  runtimeOnly("org.flywaydb:flyway-core:10.15.0")
  runtimeOnly("org.flywaydb:flyway-database-postgresql:10.15.0")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.wiremock:wiremock-standalone:3.6.0")
  testImplementation("io.jsonwebtoken:jjwt-api:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("org.jmock:jmock:2.13.1")
  testImplementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.6")
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
  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
    finalizedBy("koverHtmlReport")
  }
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
    }
  }
}
