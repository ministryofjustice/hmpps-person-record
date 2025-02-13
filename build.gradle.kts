kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "7.1.1"
  kotlin("plugin.spring") version "2.1.10"
  kotlin("jvm") version "2.1.10"
  kotlin("plugin.jpa") version "2.1.10"
  id("io.gitlab.arturbosch.detekt") version "1.23.7"
  id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.2.1")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.3.1")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("jakarta.validation:jakarta.validation-api:3.1.1")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.2.0")
  implementation("aws.sdk.kotlin:s3:1.4.19")

  implementation("io.swagger.core.v3:swagger-annotations:2.2.28")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

  constraints {
    implementation("commons-io:commons-io:2.18.0") {
      because("2.13.0 has CVEs")
    }
  }

  implementation("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

  runtimeOnly("org.postgresql:postgresql:42.7.5")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.wiremock:wiremock-standalone:3.12.0")
  testImplementation("io.jsonwebtoken:jjwt-api:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.10")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("org.jmock:jmock:2.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.2.1")
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
