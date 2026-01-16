package io.hansu.pacer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class RunningPacerApplication

fun main(args: Array<String>) {
    runApplication<RunningPacerApplication>(*args)
}
