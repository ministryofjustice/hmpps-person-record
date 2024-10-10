import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include

kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.6"
  kotlin("plugin.spring") version "2.0.20"
  kotlin("jvm") version "2.0.20"
  kotlin("plugin.jpa") version "2.0.20"
  id("io.gitlab.arturbosch.detekt") version "1.23.7"
  id("org.jetbrains.kotlinx.kover") version "0.8.3"
  id("org.gradle.test-retry") version "1.6.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.0.7")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.0.1")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("jakarta.validation:jakarta.validation-api:3.1.0")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.3")
  implementation("org.springframework.security:spring-security-config:6.3.3")
  constraints {
    implementation("commons-io:commons-io:2.17.0") {
      because("2.13.0 has CVEs")
    }
  }
  constraints {
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1") {
      because("1.77 has CVEs")
    }
  }
  implementation("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

  runtimeOnly("org.postgresql:postgresql:42.7.4")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.wiremock:wiremock-standalone:3.9.1")
  testImplementation("io.jsonwebtoken:jjwt-api:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.20")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("org.jmock:jmock:2.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.0.7")
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
    include("**/HealthCheckIntTest.class")
  }

  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
    finalizedBy("koverHtmlReport")
  }

  withType(Test::class) {
    retry {
      if (environment.get("CI") == "true") {
        maxRetries.set(3)
      }
    }
  }
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
    }
  }
}
