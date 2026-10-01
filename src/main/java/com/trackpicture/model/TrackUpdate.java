package com.trackpicture.model;

import java.time.Instant;

public record TrackUpdate(
    String messageId,
    String trackId,
    Instant timestamp,
    double latitude,
    double longitude,
    Double altitude,
    Double heading,
    Identity identity) {}
