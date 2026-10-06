package com.trackpicture;

import com.trackpicture.model.Identity;
import com.trackpicture.model.TrackUpdate;
import java.time.Instant;
import java.util.function.UnaryOperator;

public final class TrackUpdateTestSupport {

  public static final Instant T1 = Instant.parse("2026-10-01T12:00:00Z");
  public static final Instant T2 = Instant.parse("2026-10-01T12:10:00Z");

  private TrackUpdateTestSupport() {}

  public static TrackUpdate validUpdate() {
    return update(b -> b);
  }

  public static TrackUpdate update(UnaryOperator<Builder> customize) {
    var builder = new Builder();
    customize.apply(builder);
    return builder.build();
  }

  public static final class Builder {
    String messageId = "msg-1";
    String trackId = "T-001";
    Instant timestamp = T1;
    double latitude = 51.5;
    double longitude = -0.1;
    Double altitude = null;
    Double heading = null;
    Identity identity = Identity.UNKNOWN;

    public Builder messageId(String messageId) {
      this.messageId = messageId;
      return this;
    }

    public Builder trackId(String trackId) {
      this.trackId = trackId;
      return this;
    }

    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder latitude(double latitude) {
      this.latitude = latitude;
      return this;
    }

    public Builder longitude(double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder altitude(Double altitude) {
      this.altitude = altitude;
      return this;
    }

    public Builder heading(Double heading) {
      this.heading = heading;
      return this;
    }

    public Builder identity(Identity identity) {
      this.identity = identity;
      return this;
    }

    public TrackUpdate build() {
      return new TrackUpdate(
          messageId, trackId, timestamp, latitude, longitude, altitude, heading, identity);
    }
  }
}
