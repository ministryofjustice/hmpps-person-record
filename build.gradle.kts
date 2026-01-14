@file:Suppress("UnstableApiUsage")

kotlin {
  jvmToolchain(25)
  compilerOptions {
    freeCompilerArgs.add("-Xannotation-default-target=param-property")
  }
}

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("jvm") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  id("org.jetbrains.kotlinx.kover") version "0.9.4"
  id("org.owasp.dependencycheck") version "12.2.0"
}

dependencyCheck {
  nvd.datafeedUrl = "file:///opt/vulnz/cache"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:6.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("jakarta.validation:jakarta.validation-api:3.1.1")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")

  implementation("software.amazon.sns:sns-extended-client:2.1.0")
  implementation("com.jayway.jsonpath:json-path:2.10.0")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.41")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  runtimeOnly("org.postgresql:postgresql:42.7.8")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.wiremock:wiremock-standalone:3.13.2")

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.jmock:jmock:2.13.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
}

repositories {
  mavenCentral()
}

val test by testing.suites.existing(JvmTestSuite::class)

tasks.register<Test>("initialiseDatabase") {
  testClassesDirs = files(test.map { it.sources.output.classesDirs })
  classpath = files(test.map { it.sources.runtimeClasspath })
  include("**/InitialiseDatabase.class")
  onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
}

tasks.register<Test>("e2eTest") {
  testClassesDirs = files(test.map { it.sources.output.classesDirs })
  classpath = files(test.map { it.sources.runtimeClasspath })
  include("**/**E2ETest.class")
  onlyIf { gradle.startParameter.taskNames.contains("e2eTest") }
}

tasks {
  test {
    exclude("**/InitialiseDatabase.class")
    exclude("**/**E2ETest.class")
  }

  getByName("check") {
    dependsOn(":ktlintCheck")
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
