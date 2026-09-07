package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
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
class SplitClustersJob : BeanFactoryPostProcessor {

  @EventListener
  fun onApplicationEvent(event: ContextRefreshedEvent) {
    splitClusters()
    event.closeApplication()
  }

  fun splitClusters() = runBlocking {
    LOG.info("Started running splitting of clusters")

    LOG.info("Finished running splitting of clusters")
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

  private fun ContextRefreshedEvent.closeApplication() = (this.applicationContext as ConfigurableApplicationContext).close()

  companion object {
    private val LOG = LoggerFactory.getLogger(SplitClustersJob::class.java)
  }
}
