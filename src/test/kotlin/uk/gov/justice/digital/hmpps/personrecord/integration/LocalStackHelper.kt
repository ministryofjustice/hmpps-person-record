package uk.gov.justice.digital.hmpps.personrecord.integration

import org.slf4j.LoggerFactory
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.ServerSocket

object LocalStackHelper {
  val log = LoggerFactory.getLogger(this::class.java)
  val instance by lazy { startLocalstackIfNotRunning() }

  private fun startLocalstackIfNotRunning(): LocalStackContainer? {
    if (localstackIsRunning()) return null
    val logConsumer = Slf4jLogConsumer(log).withPrefix("localstack")
    return LocalStackContainer(
      DockerImageName.parse("localstack/localstack").withTag("3.0"),
    ).apply {
      withServices(LocalStackContainer.Service.SNS, LocalStackContainer.Service.SQS)
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      withEnv("DEFAULT_REGION", "eu-west-2")
      waitingFor(
        Wait.forLogMessage(".*Ready.*", 1),
      )
      start()
      followOutput(logConsumer)
    }
  }

  private fun localstackIsRunning(): Boolean =
    try {
      val serverSocket = ServerSocket(4566)
      serverSocket.localPort == 0
    } catch (e: IOException) {
      true
    }
}
