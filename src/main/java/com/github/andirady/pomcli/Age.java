package com.github.andirady.pomcli;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

public record Age(long birth) {

    public Duration asDuration() {
        var now = Instant.now();
        var past = Instant.ofEpochMilli(birth);
        return Duration.between(past, now);
    }

    public Period asPeriod() {
        return asPeriod(asDuration());
    }

    private Period asPeriod(Duration duration) {
        return Period.between(LocalDate.now().minusDays(duration.toDays()), LocalDate.now());
    }

    @Override
    public String toString() {
        var duration = asDuration();
        if (duration.toDays() > 0) {
            var period = asPeriod(duration);
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
