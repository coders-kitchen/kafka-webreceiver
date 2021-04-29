package com.coderskitchen.kafkawebreceiver

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import java.time.Duration


@Configuration
class KafkaConfig {
    @Bean
    fun producerFactory(): ProducerFactory<Int, String> {
        return DefaultKafkaProducerFactory(producerConfigs())
    }

    @Bean
    fun producerConfigs(): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "192.168.0.2:9092"
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.MAX_BLOCK_MS_CONFIG] = "1000"
        props[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = "200"
        props[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = "900"
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return props
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<Int, String> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun defaultCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer<ReactiveResilience4JCircuitBreakerFactory> { factory ->
            factory.configureDefault { id ->
                Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(CircuitBreakerConfig
                        .custom()
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build())
                    .build()
            }
        }
    }
}