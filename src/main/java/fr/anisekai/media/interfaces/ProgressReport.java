package fr.anisekai.media.interfaces;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A single progress report parsed from ffmpeg {@code -progress} output.
 *
 * @param frame
 *         The number of frames processed, or {@code 0} when unreported.
 * @param fps
 *         The current processing speed in frames per second, or {@code 0} when unreported.
 * @param outTimeUs
 *         The current output timestamp in microseconds, or {@code 0} when unreported.
 * @param speed
 *         The current processing speed relative to realtime (e.g. {@code 2.5} for {@code 2.5x}),
 *         or {@code 0} when unreported.
 * @param totalSize
 *         The total output size in bytes so far, or {@code 0} when unreported.
 * @param finished
 *         {@code true} once ffmpeg reported {@code progress=end}.
 * @param percent
 *         The completion percentage within {@code [0, 100]}, or empty when the total
 *         media duration is unknown.
 */
public record ProgressReport(
        long frame,
        double fps,
        long outTimeUs,
        double speed,
        long totalSize,
        boolean finished,
        OptionalDouble percent
) {

    /**
     * Compute the completion percentage for the provided output timestamp against the
     * total media duration.
     *
     * @param outTimeUs
     *         The current output timestamp in microseconds.
     * @param total
     *         The total media duration, if known.
     *
     * @return The clamped percentage, or empty when it cannot be computed.
     */
    public static OptionalDouble percent(long outTimeUs, Optional<Duration> total) {

        if (outTimeUs < 0 || total.isEmpty()) {
            return OptionalDouble.empty();
        }

        long totalUs = total.get().toNanos() / 1000;
        if (totalUs <= 0) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(Math.min(100.0, Math.max(0.0, (outTimeUs * 100.0) / totalUs)));
    }
}
