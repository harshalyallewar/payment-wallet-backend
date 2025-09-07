package com.pw.walletservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.walletservice.model.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendEvent(String topic, EventEnvelope event) {
        try {
            // Serialize the event envelope to JSON
            String message = objectMapper.writeValueAsString(event);

            // Send the message with eventId as the key
            kafkaTemplate.send(topic, event.getEventId(), message);

            log.info("Sent event {} to topic {}", event.getEventId(), topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage(), e);
        }
    }
}
