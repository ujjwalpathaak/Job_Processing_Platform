package job_processing_platform.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParseDuration {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)(ms|s|m|h)$", Pattern.CASE_INSENSITIVE);

    private ParseDuration() {
    }

    public static long toMillis(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid duration format: " + value);
        }

        Matcher matcher = DURATION_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration format: " + value);
        }

        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();

        return switch (unit) {
            case "ms" -> amount;
            case "s"  -> amount * 1_000L;
            case "m"  -> amount * 60_000L;
            default   -> amount * 3_600_000L;
        };
    }
}
