package uk.gov.justice.digital.hmpps.personrecord.api.model

import org.junit.jupiter.api.Test
import wiremock.com.github.jknack.handlebars.internal.Files
import java.io.File
import java.nio.file.Paths

class SwaggerAvoidSameClassNameTest {

  @Test
  fun `ensure there are not two classes with the same name with Swagger annotations`() {
    val kotlinFilesLocation = Paths.get("src/main/kotlin").toAbsolutePath()
    val ls = File(kotlinFilesLocation.toString()).walkTopDown().filter {
      it.isFile() && Files.read(it, Charsets.UTF_8).contains("@Schema")
    }.map { it.name }.groupingBy { it }.eachCount().filter { it.value > 1 }

    if (ls.isNotEmpty()) {
      ls.forEach { filename ->
        print("There is more than one class named $filename with swagger annotations")
      }
    }
  }
}
