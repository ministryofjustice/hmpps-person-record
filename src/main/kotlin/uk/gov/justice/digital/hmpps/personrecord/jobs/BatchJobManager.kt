package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class BatchJobManager(
  @param:Value($$"${batch.type}") private val jobName: String,
  @param:Value($$"${batch.exit-on-completion:true}") private val exitOnCompletion: Boolean,
  registeredBatchJobs: List<BatchJob>,
) {

  private val batchJobs = registeredBatchJobs.associateBy { it.jobName }

  @EventListener
  fun onApplicationEvent(event: ContextRefreshedEvent) = runJob().also { if (exitOnCompletion) event.closeApplication() }

  fun runJob() = runBlocking {
    LOG.info("Running batch job '{}'", jobName)
    batchJobs[jobName]
      ?.runCatching { run() }
      ?.onSuccess { LOG.info("Finished batch job '{}'", jobName) }
      ?.onFailure { LOG.error("Exception happened during batch job '{}'", jobName, it) }
      ?: LOG.error("Job '$jobName' not found")
  }

  private fun ContextRefreshedEvent.closeApplication() = (this.applicationContext as ConfigurableApplicationContext).close()

  companion object {
    private val LOG = LoggerFactory.getLogger(BatchJobManager::class.java)
  }
}

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class SqsListenerSuppressor : BeanFactoryPostProcessor {
  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    (beanFactory as DefaultListableBeanFactory).beanDefinitionNames
      .filter { beanName -> beanFactory.hasSqsListener(beanName) }
      .forEach {
        LOG.info("Removing SQS listener bean {} for batch job", it)
        beanFactory.removeBeanDefinition(it)
      }
  }

  private fun DefaultListableBeanFactory.hasSqsListener(beanName: String): Boolean {
    val beanClass = getBeanDefinition(beanName).beanClassName
      ?.let { runCatching { Class.forName(it, false, beanClassLoader) }.getOrNull() }
      ?: runCatching { getType(beanName, false) }.getOrNull()

    return beanClass
      ?.methods
      ?.any { it.isAnnotationPresent(SqsListener::class.java) } == true
  }
}
