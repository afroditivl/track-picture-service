package com.trackpicture.service;

public record ProcessResult(Outcome outcome, String reason) {

  public enum Outcome {
    ACCEPTED,
    DUPLICATE,
    REJECTED
  }

  public static ProcessResult accepted() {
    return new ProcessResult(Outcome.ACCEPTED, null);
  }

  public static ProcessResult duplicate() {
    return new ProcessResult(Outcome.DUPLICATE, null);
  }

  public static ProcessResult rejected(String reason) {
    return new ProcessResult(Outcome.REJECTED, reason);
  }
}
