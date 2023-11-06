plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.8.0"
  kotlin("plugin.spring") version "1.9.0"
  kotlin("jvm") version "1.9.0"
  kotlin("plugin.jpa") version "1.9.0"
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
  implementation("jakarta.validation:jakarta.validation-api:3.0.2")
  implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.4")
  implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")
  implementation("org.hibernate.orm:hibernate-envers:6.3.1.Final")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.18")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
  implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.0.4")
  implementation("org.springframework.cloud:spring-cloud-dependencies:2022.0.4")

  runtimeOnly("org.postgresql:postgresql:42.5.4")
  runtimeOnly("org.flywaydb:flyway-core:9.22.3")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:junit-jupiter:1.19.1")
  testImplementation("org.testcontainers:postgresql:1.19.1")
  testImplementation("org.wiremock:wiremock:3.2.0")
  testImplementation("io.jsonwebtoken:jjwt-api:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.20")
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
