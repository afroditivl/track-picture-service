package com.trackpicture.service;

import com.trackpicture.model.Identity;
import com.trackpicture.model.TrackUpdate;
import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class HistoryRepository {

  private static final RowMapper<TrackUpdate> ROW_MAPPER =
      (rs, rowNum) ->
          new TrackUpdate(
              rs.getString("message_id"),
              rs.getString("track_id"),
              rs.getTimestamp("observed_at").toInstant(),
              rs.getDouble("latitude"),
              rs.getDouble("longitude"),
              (Double) rs.getObject("altitude"),
              (Double) rs.getObject("heading"),
              Identity.valueOf(rs.getString("identity")));

  private final JdbcTemplate jdbc;

  public HistoryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @PostConstruct
  void initSchema() {
    jdbc.execute(
        """
            CREATE TABLE IF NOT EXISTS track_history (
                message_id VARCHAR(255) PRIMARY KEY,
                track_id VARCHAR(255) NOT NULL,
                observed_at TIMESTAMPTZ NOT NULL,
                latitude DOUBLE PRECISION NOT NULL,
                longitude DOUBLE PRECISION NOT NULL,
                altitude DOUBLE PRECISION,
                heading DOUBLE PRECISION,
                identity VARCHAR(32) NOT NULL
            )
            """);
    jdbc.execute(
        "CREATE INDEX IF NOT EXISTS idx_track_history_track_id ON track_history(track_id)");
  }

  public boolean insert(TrackUpdate update) {
    int rows =
        jdbc.update(
            """
                INSERT INTO track_history (
                    message_id, track_id, observed_at, latitude, longitude,
                    altitude, heading, identity
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (message_id) DO NOTHING
                """,
            update.messageId(),
            update.trackId(),
            Timestamp.from(update.timestamp()),
            update.latitude(),
            update.longitude(),
            update.altitude(),
            update.heading(),
            update.identity().name());
    return rows == 1;
  }

  public List<TrackUpdate> findByTrackId(String trackId, Instant from, Instant to) {
    var sql =
        new StringBuilder(
            """
                        SELECT message_id, track_id, observed_at, latitude, longitude,
                               altitude, heading, identity
                        FROM track_history
                        WHERE track_id = ?
                        """);
    var args = new ArrayList<Object>();
    args.add(trackId);
    if (from != null) {
      sql.append(" AND observed_at >= ?");
      args.add(Timestamp.from(from));
    }
    if (to != null) {
      sql.append(" AND observed_at <= ?");
      args.add(Timestamp.from(to));
    }
    sql.append(" ORDER BY observed_at ASC");
    return jdbc.query(sql.toString(), ROW_MAPPER, args.toArray());
  }
}
