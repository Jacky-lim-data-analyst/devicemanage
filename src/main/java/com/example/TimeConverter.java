package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeConverter {
    public static String convertMilis(long milis) {
        // calculate each time unit
        long days = TimeUnit.MILLISECONDS.toDays(milis);
        milis -= TimeUnit.DAYS.toMillis(days);

        long hours = TimeUnit.MILLISECONDS.toHours(milis);
        milis -= TimeUnit.HOURS.toMillis(hours);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(milis);
        milis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(milis);

        List<String> parts = new ArrayList<>();
        addPart(parts, days, "day");
        addPart(parts, hours, "hour");
        addPart(parts, minutes, "minute");
        addPart(parts, seconds, "second");

        return parts.isEmpty() ? "0 second" : String.join(", ", parts);
    }

    private static void addPart(List<String> parts, long value, String unit) {
        if (value > 0) {
            parts.add(value + " " + unit + (value != 1 ? "s" : ""));
        }
    }
}
