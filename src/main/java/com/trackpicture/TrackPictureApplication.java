package com.trackpicture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class TrackPictureApplication {

  public static void main(String[] args) {
    SpringApplication.run(TrackPictureApplication.class, args);
  }
}
