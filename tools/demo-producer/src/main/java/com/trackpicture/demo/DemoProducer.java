package com.trackpicture.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public final class DemoProducer {

  private static final String TOPIC = "track-updates";
  private static final String BOOTSTRAP =
      System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");

  public static void main(String[] args) throws Exception {
    var props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.ACKS_CONFIG, "all");

    var mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    try (var producer = new KafkaProducer<String, String>(props)) {
      System.out.println(
          "Start Publishing to topic " + TOPIC + " (" + BOOTSTRAP + ")");

      // Normal motion
      publish(
          producer,
          mapper,
          "T-001",
          "msg-t001-a",
          "2026-10-01T12:00:00Z",
          51.5,
          -0.12,
          "Normal T-001 update A");
      publish(
          producer,
          mapper,
          "T-001",
          "msg-t001-b",
          "2026-10-01T12:01:00Z",
          51.51,
          -0.11,
          "Normal T-001 update B (newer picture)");

      // Duplicate messageId - must not duplicate history or change picture
      publish(
          producer,
          mapper,
          "T-001",
          "msg-t001-b",
          "2026-10-01T12:01:00Z",
          51.51,
          -0.11,
          "DUPLICATE: same messageId as previous");

      // Out-of-order (picture stays newer)
      publish(
          producer,
          mapper,
          "T-002",
          "msg-t002-newer",
          "2026-10-01T12:10:00Z",
          48.8,
          2.3,
          "T-002 newer timestamp first");
      publish(
          producer,
          mapper,
          "T-002",
          "msg-t002-older",
          "2026-10-01T12:05:00Z",
          48.7,
          2.2,
          "OUT-OF-ORDER: older timestamp after newer");

      // Second track normal
      publish(
          producer,
          mapper,
          "T-003",
          "msg-t003-a",
          "2026-10-01T12:00:30Z",
          40.4,
          -3.7,
          "Normal T-003");

      // Invalid - bad latitude (rejected, logged only)
      publishRaw(
          producer,
          "T-004",
          """
              {"messageId":"msg-invalid","trackId":"T-004","timestamp":"2026-10-01T12:00:00Z",\
              "latitude":999,"longitude":0,"identity":"HOSTILE"}\
              """,
          "INVALID: latitude out of range");

      producer.flush();
      System.out.println(
          "Publishing done");
    }
  }

  private static void publish(
      KafkaProducer<String, String> producer,
      ObjectMapper mapper,
      String trackId,
      String messageId,
      String timestamp,
      double latitude,
      double longitude,
      String label)
      throws Exception {
    var payload =
        Map.of(
            "messageId", messageId,
            "trackId", trackId,
            "timestamp", timestamp,
            "latitude", latitude,
            "longitude", longitude,
            "identity", "UNKNOWN");
    publishRaw(producer, trackId, mapper.writeValueAsString(payload), label);
  }

  private static void publishRaw(
      KafkaProducer<String, String> producer, String trackId, String json, String label)
      throws Exception {
    System.out.println(" -> " + label);
    producer.send(new ProducerRecord<>(TOPIC, trackId, json)).get();
    Thread.sleep(300);
  }

  private DemoProducer() {}
}
