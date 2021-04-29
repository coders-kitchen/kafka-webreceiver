package com.coderskitchen.kafkawebreceiver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaWebreceiverApplication

fun main(args: Array<String>) {
    runApplication<KafkaWebreceiverApplication>(*args)
}
