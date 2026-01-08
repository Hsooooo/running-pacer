package io.hansu.pacer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RunningPacerApplication

fun main(args: Array<String>) {
    runApplication<RunningPacerApplication>(*args)
}
