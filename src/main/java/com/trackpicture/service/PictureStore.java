package com.trackpicture.service;

import com.trackpicture.model.TrackUpdate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PictureStore {

  private final ConcurrentHashMap<String, TrackUpdate> byTrackId = new ConcurrentHashMap<>();

  public Optional<TrackUpdate> get(String trackId) {
    return Optional.ofNullable(byTrackId.get(trackId));
  }

  public List<TrackUpdate> getAll() {
    return List.copyOf(byTrackId.values());
  }

  public void put(TrackUpdate update) {
    byTrackId.put(update.trackId(), update);
  }
}
