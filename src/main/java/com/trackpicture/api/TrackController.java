package com.trackpicture.api;

import com.trackpicture.model.TrackUpdate;
import com.trackpicture.service.HistoryRepository;
import com.trackpicture.service.PictureStore;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TrackController {

  private final PictureStore pictureStore;
  private final HistoryRepository historyRepository;

  public TrackController(PictureStore pictureStore, HistoryRepository historyRepository) {
    this.pictureStore = pictureStore;
    this.historyRepository = historyRepository;
  }

  @GetMapping("/picture")
  public List<TrackUpdate> getPicture() {
    return pictureStore.getAll();
  }

  @GetMapping("/tracks/{trackId}")
  public ResponseEntity<TrackUpdate> getTrack(@PathVariable String trackId) {
    return pictureStore
        .get(trackId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/tracks/{trackId}/history")
  public List<TrackUpdate> getHistory(
      @PathVariable String trackId,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    return historyRepository.findByTrackId(
        trackId, parseInstantParam(from, "from"), parseInstantParam(to, "to"));
  }

  private static Instant parseInstantParam(String value, String paramName) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid %s timestamp: must be ISO-8601 UTC".formatted(paramName));
    }
  }
}
