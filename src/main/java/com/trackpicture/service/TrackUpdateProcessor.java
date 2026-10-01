package com.trackpicture.service;

import com.trackpicture.model.TrackUpdate;
import com.trackpicture.validation.TrackUpdateValidator;
import com.trackpicture.validation.ValidationResult;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TrackUpdateProcessor {

  private final TrackUpdateValidator validator;
  private final PictureStore pictureStore;
  private final HistoryRepository historyRepository;
  private final ConcurrentHashMap<String, Object> trackLocks = new ConcurrentHashMap<>();

  public TrackUpdateProcessor(
      TrackUpdateValidator validator,
      PictureStore pictureStore,
      HistoryRepository historyRepository) {
    this.validator = validator;
    this.pictureStore = pictureStore;
    this.historyRepository = historyRepository;
  }

  public ProcessResult process(TrackUpdate update) {
    ValidationResult validation = validator.validate(update);
    if (!validation.valid()) {
      return ProcessResult.rejected(validation.reason());
    }

    Object lock = trackLocks.computeIfAbsent(update.trackId(), ignored -> new Object());
    synchronized (lock) {
      if (!historyRepository.insert(update)) {
        return ProcessResult.duplicate();
      }
      updatePicture(update);
      return ProcessResult.accepted();
    }
  }

  private void updatePicture(TrackUpdate update) {
    pictureStore
        .get(update.trackId())
        .ifPresentOrElse(
            current -> {
              if (update.timestamp().isAfter(current.timestamp())) {
                pictureStore.put(update);
              }
            },
            () -> pictureStore.put(update));
  }
}
