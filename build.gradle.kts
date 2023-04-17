plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.1.3-beta"
  kotlin("plugin.spring") version "1.8.20-RC"
  kotlin("jvm") version "1.8.20-RC"
  kotlin("plugin.jpa") version "1.8.20-RC"
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
  implementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:4.0.2")
  implementation("jakarta.validation:jakarta.validation-api:3.0.2")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")
  implementation("org.hibernate.orm:hibernate-envers:6.1.7.Final")

  runtimeOnly("org.postgresql:postgresql:42.5.4")
  runtimeOnly("org.flywaydb:flyway-core:9.16.0")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:junit-jupiter:1.17.6")
  testImplementation("org.testcontainers:postgresql:1.17.6")
  testImplementation("com.github.tomakehurst:wiremock:3.0.0-beta-7")
  testImplementation("io.jsonwebtoken:jjwt-api:0.11.5")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.11.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.8.20-RC")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

repositories {
  mavenCentral()
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
}
