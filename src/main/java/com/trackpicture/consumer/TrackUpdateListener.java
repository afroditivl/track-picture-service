package com.trackpicture.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackpicture.model.TrackUpdate;
import com.trackpicture.service.ProcessResult;
import com.trackpicture.service.TrackUpdateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TrackUpdateListener {

  private static final Logger log = LoggerFactory.getLogger(TrackUpdateListener.class);

  private static final String TOPIC = "track-updates";

  private final ObjectMapper objectMapper;
  private final TrackUpdateProcessor processor;

  public TrackUpdateListener(ObjectMapper objectMapper, TrackUpdateProcessor processor) {
    this.objectMapper = objectMapper;
    this.processor = processor;
  }

  @KafkaListener(topics = TOPIC)
  public void onMessage(String payload) {
    TrackUpdate update;
    try {
      update = objectMapper.readValue(payload, TrackUpdate.class);
    } catch (Exception e) {
      log.warn("Rejected track update: invalid JSON ({})", e.getMessage());
      return;
    }

    ProcessResult result = processor.process(update);
    if (result.outcome() == ProcessResult.Outcome.REJECTED) {
      log.warn("Rejected track update: {}", result.reason());
    }
  }
}
