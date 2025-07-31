package com.toktot.common.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateTimeUtil {

    public static LocalDateTime nowWithoutNanos() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public static LocalDateTime removeNanos(LocalDateTime dateTime) {
        return dateTime.truncatedTo(ChronoUnit.SECONDS);
    }
}
