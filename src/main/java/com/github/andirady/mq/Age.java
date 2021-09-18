package com.github.andirady.mq;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

public record Age(long birth) {

    @Override
    public String toString() {
        var now = Instant.now();
        var past = Instant.ofEpochMilli(birth);
        var duration = Duration.between(past, now);
        if (duration.toDaysPart() > 0) {
            var zoneId = ZoneId.systemDefault();
            var period = Period.between(LocalDate.ofInstant(past, zoneId), LocalDate.ofInstant(now, zoneId));
            if (period.getYears() > 0) {
                return ago(period.getYears(), "a year");
            } else if (period.getMonths() > 0) {
                return ago(period.getMonths(), "a month");
            }

            return ago(period.getDays(), "a day");
        } else if (duration.toHoursPart() > 0) {
            return ago(duration.toHoursPart(), "an hour");
        } else if (duration.toMinutesPart() > 0) {
            return ago(duration.toMinutesPart(), "a minute");
        }

        return "just now";
    }

    private String ago(int n, String singular) {
        if (n == 1) {
            return singular + " ago";
        }
        return n + singular.substring(singular.indexOf(' ')) + "s ago";
    }
}
