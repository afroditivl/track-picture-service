package com.trackpicture.service;

import static com.trackpicture.TrackUpdateTestSupport.T1;
import static com.trackpicture.TrackUpdateTestSupport.T2;
import static com.trackpicture.TrackUpdateTestSupport.update;
import static com.trackpicture.TrackUpdateTestSupport.validUpdate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackpicture.validation.TrackUpdateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackUpdateProcessorTest {

  @Mock private HistoryRepository historyRepository;

  private PictureStore pictureStore;
  private TrackUpdateProcessor processor;

  @BeforeEach
  void setUp() {
    pictureStore = new PictureStore();
    processor =
        new TrackUpdateProcessor(new TrackUpdateValidator(), pictureStore, historyRepository);
  }

  @Test
  void acceptsValidUpdateAndUpdatesPicture() {
    var update = validUpdate();
    when(historyRepository.insert(update)).thenReturn(true);

    var result = processor.process(update);

    assertEquals(ProcessResult.Outcome.ACCEPTED, result.outcome());
    assertTrue(pictureStore.get(update.trackId()).isPresent());
    assertEquals(update, pictureStore.get(update.trackId()).orElseThrow());
  }

  @Test
  void duplicateMessageIdDoesNotChangePicture() {
    var update = validUpdate();
    when(historyRepository.insert(update)).thenReturn(true, false);

    processor.process(update);
    var result = processor.process(update);

    assertEquals(ProcessResult.Outcome.DUPLICATE, result.outcome());
    assertEquals(1, pictureStore.getAll().size());
  }

  @Test
  void outOfOrderUpdateKeepsNewerPictureButIsAccepted() {
    var newer = update(b -> b.messageId("newer").timestamp(T2).latitude(48.8));
    var older = update(b -> b.messageId("older").timestamp(T1).latitude(48.7));
    when(historyRepository.insert(any())).thenReturn(true);

    processor.process(newer);
    var result = processor.process(older);

    assertEquals(ProcessResult.Outcome.ACCEPTED, result.outcome());
    assertEquals(newer, pictureStore.get("T-001").orElseThrow());
    verify(historyRepository, times(2)).insert(any());
  }

  @Test
  void rejectedUpdateDoesNotTouchHistoryOrPicture() {
    var invalid = update(b -> b.latitude(999));

    var result = processor.process(invalid);

    assertEquals(ProcessResult.Outcome.REJECTED, result.outcome());
    verify(historyRepository, never()).insert(any());
    assertTrue(pictureStore.getAll().isEmpty());
  }
}
