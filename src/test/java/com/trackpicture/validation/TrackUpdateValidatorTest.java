package com.trackpicture.validation;

import static com.trackpicture.TrackUpdateTestSupport.update;
import static com.trackpicture.TrackUpdateTestSupport.validUpdate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackpicture.model.Identity;
import com.trackpicture.model.TrackUpdate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TrackUpdateValidatorTest {

  private final TrackUpdateValidator validator = new TrackUpdateValidator();

  @ParameterizedTest
  @MethodSource("invalidUpdates")
  void rejectsInvalidUpdate(TrackUpdate update, String expectedReason) {
    ValidationResult result = validator.validate(update);

    assertFalse(result.valid());
    assertTrue(result.reason().contains(expectedReason) || result.reason().equals(expectedReason));
  }

  @ParameterizedTest
  @MethodSource("validUpdates")
  void acceptsValidUpdate(TrackUpdate update) {
    assertTrue(validator.validate(update).valid());
  }

  static Stream<Arguments> invalidUpdates() {
    return Stream.of(
        Arguments.of(null, "update is null"),
        Arguments.of(update(b -> b.messageId("")), "messageId is required"),
        Arguments.of(update(b -> b.trackId("  ")), "trackId is required"),
        Arguments.of(update(b -> b.timestamp(null)), "timestamp is required"),
        Arguments.of(update(b -> b.latitude(91)), "latitude"),
        Arguments.of(update(b -> b.longitude(-181)), "longitude"),
        Arguments.of(update(b -> b.altitude(-1.0)), "altitude"),
        Arguments.of(update(b -> b.heading(360.0)), "heading"),
        Arguments.of(update(b -> b.identity(null)), "identity is required"));
  }

  static Stream<TrackUpdate> validUpdates() {
    return Stream.of(
        validUpdate(),
        update(b -> b.altitude(1000.0).heading(90.0).identity(Identity.HOSTILE)));
  }
}
