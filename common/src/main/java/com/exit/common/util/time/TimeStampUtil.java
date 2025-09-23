package com.exit.common.util.time;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class TimeStampUtil {

    public static LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) return null;

        Instant instant = Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Timestamp toGrpcTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
