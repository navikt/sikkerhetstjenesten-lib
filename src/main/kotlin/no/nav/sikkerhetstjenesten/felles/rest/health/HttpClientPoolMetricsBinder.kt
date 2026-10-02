package no.nav.sikkerhetstjenesten.felles.rest.health

import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PatchExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange

class HttpClientPoolMetricsBinder(
    private val metrics: HttpClientPoolMetrics,
    private val beanFactory: ListableBeanFactory) : SmartInitializingSingleton {

    override fun afterSingletonsInstantiated() {
        beanFactory.getBeansOfType(RestClient::class.java)
            .forEach { (name, client) -> metrics.bind(name, client) }
        beanFactory.getBeansOfType(HttpComponentsClientHttpRequestFactory::class.java)
            .forEach { (name, factory) -> metrics.bind(name, factory) }
        beanFactory.beanDefinitionNames.forEach { beanName ->
            val type = beanFactory.getType(beanName) ?: return@forEach
            if (isHttpExchangeClientType(type)) metrics.bindClient(beanName)
        }
    }

    private fun isHttpExchangeClientType(type: Class<*>) =
        buildList {
            add(type)
            addAll(type.interfaces)
        }.any { candidate ->
            candidate.methods.any { method ->
                method.isAnnotationPresent(HttpExchange::class.java) ||
                        method.isAnnotationPresent(GetExchange::class.java) ||
                        method.isAnnotationPresent(PostExchange::class.java) ||
                        method.isAnnotationPresent(PutExchange::class.java) ||
                        method.isAnnotationPresent(PatchExchange::class.java) ||
                        method.isAnnotationPresent(DeleteExchange::class.java)
            }
        }
}
