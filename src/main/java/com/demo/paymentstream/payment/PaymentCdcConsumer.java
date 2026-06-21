package com.demo.paymentstream.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCdcConsumer {

    private final PaymentServiceElasticsearch paymentServiceElasticsearch;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${debezium.topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "paymentCdcListenerContainerFactory")
    public void consume(String message) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(message).get("payload");
        String op = payload.get("op").asText();

        switch (op) {
            case "c", "u", "r" -> paymentServiceElasticsearch.upsert(payload.get("after"));
            case "d" -> paymentServiceElasticsearch.delete(payload.get("before").get("id").asText());
            default -> log.debug("Skipping CDC op: {}", op);
        }
    }
}
