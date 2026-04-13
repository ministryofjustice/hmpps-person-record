plugins {
    kotlin("jvm") version "2.3.0"
    id("io.gatling.gradle") version "3.15.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    gatling("org.postgresql:postgresql:42.7.7")
}

kotlin {
    jvmToolchain(25)
}

application{
    mainClass.set("com.moj.cpr.perf.helper.CsvGenerator")
}

tasks.register<JavaExec>("generateTestData") {
    group = "application"
    classpath = sourceSets.getByName("gatling").runtimeClasspath
    mainClass.set("com.moj.cpr.perf.helper.CsvGenerator")
}