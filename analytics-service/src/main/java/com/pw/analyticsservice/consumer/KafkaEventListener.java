package com.pw.analyticsservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.analyticsservice.model.EventEnvelope;
import com.pw.analyticsservice.service.EventProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventListener {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventListener.class);

    @Value("${analytics.topics.user}")
    private String userTopic;
    @Value("${analytics.topics.wallet}")
    private String walletTopic;
    @Value("${analytics.topics.transaction}")
    private String txnTopic;
    @Value("${analytics.topics.auth}")
    private String authTopic;

    private final ObjectMapper objectMapper;
    private final EventProcessor processor;

    public KafkaEventListener(ObjectMapper objectMapper, EventProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @KafkaListener(topics = {"#{'${analytics.topics.user}'}",
            "#{'${analytics.topics.wallet}'}",
            "#{'${analytics.topics.transaction}'}",
            "#{'${analytics.topics.auth}'}"},
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String value = record.value();
            EventEnvelope env = objectMapper.readValue(value, EventEnvelope.class);
            processor.process(env);
            ack.acknowledge();
        } catch (Exception ex) {
            // DO NOT ack; let it retry (or configure DLT in Kafka if needed)
            log.error("Failed to process message at offset {}: {}", record.offset(), ex.getMessage(), ex);
        }
    }
}
