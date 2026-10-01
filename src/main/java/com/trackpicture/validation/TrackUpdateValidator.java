package com.trackpicture.validation;

import com.trackpicture.model.TrackUpdate;
import org.springframework.stereotype.Component;

@Component
public class TrackUpdateValidator {

  public ValidationResult validate(TrackUpdate update) {
    if (update == null) {
      return ValidationResult.reject("update is null");
    }
    if (isBlank(update.messageId())) {
      return ValidationResult.reject("messageId is required");
    }
    if (isBlank(update.trackId())) {
      return ValidationResult.reject("trackId is required");
    }
    if (update.timestamp() == null) {
      return ValidationResult.reject("timestamp is required");
    }
    if (update.latitude() < -90 || update.latitude() > 90) {
      return ValidationResult.reject("latitude must be between -90 and 90");
    }
    if (update.longitude() < -180 || update.longitude() > 180) {
      return ValidationResult.reject("longitude must be between -180 and 180");
    }
    if (update.altitude() != null && update.altitude() < 0) {
      return ValidationResult.reject("altitude must be >= 0");
    }
    if (update.heading() != null && (update.heading() < 0 || update.heading() >= 360)) {
      return ValidationResult.reject("heading must be >= 0 and < 360");
    }
    if (update.identity() == null) {
      return ValidationResult.reject("identity is required");
    }
    return ValidationResult.ok();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
