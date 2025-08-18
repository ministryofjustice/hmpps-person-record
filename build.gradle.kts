kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    freeCompilerArgs.add("-Xannotation-default-target=param-property")
  }
}

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.6"
  kotlin("plugin.spring") version "2.2.10"
  kotlin("jvm") version "2.2.10"
  kotlin("plugin.jpa") version "2.2.10"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.5.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.10")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("jakarta.validation:jakarta.validation-api:3.1.1")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")

  implementation("software.amazon.sns:sns-extended-client:2.1.0")
  implementation("com.jayway.jsonpath:json-path:2.9.0")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.35")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.3")

  runtimeOnly("org.postgresql:postgresql:42.7.7")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.wiremock:wiremock-standalone:3.13.1")

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.jmock:jmock:2.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.5.0")
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

  register("e2eTest", Test::class) {
    include("**/**E2ETest.class")
  }

  test {
    exclude("**/InitialiseDatabase.class")
    exclude("**/**E2ETest.class")
  }

  getByName("initialiseDatabase") {
    onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
  }

  getByName("e2eTest") {
    onlyIf { gradle.startParameter.taskNames.contains("e2eTest") }
  }

  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
  }

  getByName("koverHtmlReport") {
    dependsOn("check")
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
