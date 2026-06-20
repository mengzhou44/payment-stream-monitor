package com.demo.paymentstream.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentDlqService {

    private static final String REPLAY_GROUP = "dlq-replay-group";

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${debezium.topic}")
    private String originalTopic;

    @Value("${debezium.dlq-topic}")
    private String dlqTopic;

    @KafkaListener(topics = "${debezium.dlq-topic}", groupId = "payment-dlq-monitor")
    public void onDlqMessage(String message) {
        log.warn("ALERT: Message landed in DLQ — Elasticsearch may be down. " +
                 "Bring Elasticsearch back up, then call POST /api/payments/dlq/replay");
    }

    public int replay() {
        int count = 0;
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, REPLAY_GROUP,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
        );

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            TopicPartition partition = new TopicPartition(dlqTopic, 0);
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            for (var record : records) {
                kafkaTemplate.send(originalTopic, record.key(), record.value());
                count++;
            }

            // commit offsets so count() reflects what has already been replayed
            if (count > 0) {
                List<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                long nextOffset = partitionRecords.get(partitionRecords.size() - 1).offset() + 1;
                consumer.commitSync(Map.of(partition, new OffsetAndMetadata(nextOffset)));
            }
        }

        log.info("Replayed {} message(s) from DLQ back to {}", count, originalTopic);
        return count;
    }

    public long count() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, REPLAY_GROUP,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            TopicPartition partition = new TopicPartition(dlqTopic, 0);
            consumer.assign(List.of(partition));

            long endOffset = consumer.endOffsets(List.of(partition)).get(partition);
            OffsetAndMetadata committed = consumer.committed(java.util.Set.of(partition)).get(partition);
            long committedOffset = committed != null ? committed.offset() : 0;

            return endOffset - committedOffset;
        }
    }
}
