plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.1.2"
  kotlin("plugin.spring") version "1.8.20-RC"
  kotlin("jvm") version "1.8.10"
  kotlin("plugin.jpa") version "1.8.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("jakarta.validation:jakarta.validation-api:3.0.2")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4");

  runtimeOnly("org.postgresql:postgresql:42.5.4")
  runtimeOnly("org.flywaydb:flyway-core:9.16.0")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:junit-jupiter:1.17.6")
  testImplementation("org.testcontainers:postgresql:1.17.6")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
}
