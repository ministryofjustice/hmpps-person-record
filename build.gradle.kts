kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.9"
  kotlin("plugin.spring") version "2.0.21"
  kotlin("jvm") version "2.0.21"
  kotlin("plugin.jpa") version "2.0.21"
  id("io.gitlab.arturbosch.detekt") version "1.23.7"
  id("org.jetbrains.kotlinx.kover") version "0.8.3"
  id("org.gradle.test-retry") version "1.6.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.0.8")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.1.1")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("jakarta.validation:jakarta.validation-api:3.1.0")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.3")

  implementation("io.swagger.core.v3:swagger-annotations:2.2.26")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

  constraints {
    implementation("commons-io:commons-io:2.18.0") {
      because("2.13.0 has CVEs")
    }
  }

  implementation("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

  runtimeOnly("org.postgresql:postgresql:42.7.4")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.wiremock:wiremock-standalone:3.9.2")
  testImplementation("io.jsonwebtoken:jjwt-api:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.21")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("org.jmock:jmock:2.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.0.8")
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

  test {
    exclude("**/InitialiseDatabase.class")
  }

  getByName("initialiseDatabase") {
    onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
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
