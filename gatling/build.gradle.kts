plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.gatling.gradle") version "3.15.0.1"
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {

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

tasks.register<Exec>("gatlingRunCi") {
  group = "gatling"
  description = "Run un-attended in github ci"

  val args = mutableListOf("gatlingRun")
  args += listOf("--all")

  workingDir = project.rootDir
  val wrapper = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "gradlew.bat" else "./gradlew"
  println("[GATLING][Gradle] $wrapper ${args.joinToString(" ")}")
  commandLine(wrapper, *args.toTypedArray())
}