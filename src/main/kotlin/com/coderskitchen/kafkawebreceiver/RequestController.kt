package com.coderskitchen.kafkawebreceiver

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.retry.annotation.CircuitBreaker
import org.springframework.util.concurrent.FailureCallback
import org.springframework.util.concurrent.SuccessCallback
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import kotlin.random.Random


@RestController()
@RequestMapping("/message")
class RequestController(val kafkaTemplate: KafkaTemplate<Int, String>, val reactiveCircuitBreaker: ReactiveResilience4JCircuitBreakerFactory) {

    @PostMapping
    @ResponseBody
    fun messageMe(): Mono<ResponseEntity<String>> {
        val randomString = Random.nextInt().toString()


        val mono = Mono.fromFuture { kafkaTemplate.send("test-topic", randomString).completable() }

        return reactiveCircuitBreaker
            .create("kafka")
            .run(mono.map { ResponseEntity.ok(randomString) })
            .onErrorResume { Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(it.javaClass.toString() + " " +it.message)) }
    }
}