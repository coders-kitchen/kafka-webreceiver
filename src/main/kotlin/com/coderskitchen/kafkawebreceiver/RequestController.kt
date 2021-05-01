package com.coderskitchen.kafkawebreceiver

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.vault.core.VaultOperations
import org.springframework.vault.support.Plaintext
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*


@RestController()
@RequestMapping("/message")
class RequestController(val kafkaTemplate: KafkaTemplate<Int, String>,
                        val reactiveCircuitBreaker: ReactiveResilience4JCircuitBreakerFactory,
                        val vaultOperations: VaultOperations) {

    @PostMapping
    @ResponseBody
    fun messageMe(@RequestBody() body: String): Mono<ResponseEntity<String>> {
        val oft = vaultOperations.opsForTransit()
        val mono = Mono.just(Plaintext.of(body) )
            .map { oft.encrypt("orders", it).ciphertext }
            .flatMap { Mono.fromFuture { kafkaTemplate.send("test-topic2", it).completable() } }


        return submitToKafka(mono)
    }

    private fun submitToKafka(mono: Mono<SendResult<Int, String>>) =
        reactiveCircuitBreaker
            .create("kafka")
            .run(mono.map { ResponseEntity.ok(UUID.randomUUID().toString()) })
            .doOnError { it.printStackTrace() }
            .onErrorResume {
                Mono.just(
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(it.javaClass.toString() + " " + it.message)
                )
            }

    @PostMapping(path = ["/unencrypted"])
    @ResponseBody
    fun messageMeUncrypted(@RequestBody() body: String): Mono<ResponseEntity<String>> {
        val mono: Mono<SendResult<Int, String>> = Mono.fromFuture { kafkaTemplate.send("test-topic2", body).completable() }


        return submitToKafka(mono)
    }

}