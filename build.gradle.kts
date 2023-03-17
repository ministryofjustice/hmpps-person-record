plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.1.2"
  kotlin("plugin.spring") version "1.8.20-RC"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("jakarta.validation:jakarta.validation-api:3.0.2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
    }
  }
}
