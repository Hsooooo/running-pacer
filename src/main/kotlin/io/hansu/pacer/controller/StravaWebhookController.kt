package io.hansu.pacer.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.hansu.pacer.config.StravaProps
import io.hansu.pacer.dto.StravaWebhookEvent
import io.hansu.pacer.service.WebhookService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class StravaWebhookController(
    private val props: StravaProps,
    private val webhookService: WebhookService
) {
    private val om = jacksonObjectMapper()

    @GetMapping("/webhook/strava")
    fun verify(
        @RequestParam("hub.mode") mode: String,
        @RequestParam("hub.verify_token") token: String,
        @RequestParam("hub.challenge") challenge: String
    ): Map<String, String> {
        if (mode == "subscribe" && token == props.webhookVerifyToken) {
            return mapOf("hub.challenge" to challenge)
        }
        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }

    @PostMapping("/webhook/strava")
    fun handleEvent(@RequestBody body: JsonNode): ResponseEntity<String> {
        val event = om.treeToValue(body, StravaWebhookEvent::class.java)
        webhookService.processWebhookEvent(event, body.toString())
        return ResponseEntity.ok("OK")
    }
}