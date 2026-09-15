package fr.anisekai.media.bin.wrapper.tasks;

import fr.anisekai.media.interfaces.ProgressListener;
import fr.anisekai.media.interfaces.ProgressReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls an ffmpeg {@code -progress} file and forwards parsed reports to a {@link ProgressListener}.
 * <p>
 * Monitoring is auxiliary: a failing listener or unreadable file stops the monitoring without
 * ever failing the conversion being watched.
 */
public final class ProgressMonitor {

    private static final long POLL_MILLIS = 250;

    private final Path               file;
    private final Optional<Duration> total;
    private final ProgressListener   listener;
    private final AtomicBoolean      stopped = new AtomicBoolean(false);
    private final Thread             thread;
    private       int                emitted;

    /**
     * Create a new {@link ProgressMonitor}. Call {@link #start()} to begin polling.
     *
     * @param file
     *         The {@code -progress} file written by ffmpeg.
     * @param total
     *         The total media duration, used to compute completion percentages.
     * @param listener
     *         The listener receiving parsed reports on the monitor thread.
     */
    public ProgressMonitor(Path file, Optional<Duration> total, ProgressListener listener) {

        this.file     = file;
        this.total    = total;
        this.listener = listener;
        this.thread   = Thread.ofPlatform().daemon().name("ffmpeg-progress").unstarted(this::watch);
    }

    /**
     * Start polling the progress file on a daemon thread.
     */
    public void start() {

        this.thread.start();
    }

    /**
     * Stop polling and wait for the monitor thread to terminate, then emit any block
     * written since the last poll so no report is lost. Never throws and never affects
     * the conversion being watched.
     */
    public void close() {

        this.stopped.set(true);
        this.thread.interrupt();
        try {
            this.thread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            this.emitNewBlocks();
        } catch (Exception e) {
            // Best effort drain: monitoring must not fail the conversion.
        }
    }

    private void watch() {

        while (!this.stopped.get()) {
            try {
                if (this.emitNewBlocks()) {
                    return;
                }
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                // Unreadable file for now (ffmpeg may not have created it yet): retry next round.
            }
        }
    }

    /**
     * Emit every completed block not yet forwarded.
     *
     * @return {@code true} when monitoring should stop (finished block seen or listener failed).
     *
     * @throws IOException
     *         When the progress file cannot be read.
     */
    private boolean emitNewBlocks() throws IOException {

        List<Map<String, String>> blocks = readBlocks();
        while (this.emitted < blocks.size()) {
            ProgressReport report = parseBlock(blocks.get(this.emitted), this.total);
            this.emitted++;
            try {
                this.listener.onProgress(report);
            } catch (Exception e) {
                return true;
            }
            if (report.finished()) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, String>> readBlocks() throws IOException {

        if (!Files.isRegularFile(this.file)) {
            return List.of();
        }
        return splitBlocks(Files.readAllLines(this.file));
    }

    /**
     * Split raw {@code -progress} lines into one map per completed block. A block is
     * complete once its {@code progress} line is seen; a trailing partial block is ignored
     * until ffmpeg flushes it.
     *
     * @param lines
     *         The raw lines read from the progress file.
     *
     * @return One ordered map per completed block.
     */
    static List<Map<String, String>> splitBlocks(List<String> lines) {

        List<Map<String, String>> blocks = new ArrayList<>();
        Map<String, String>       block  = new LinkedHashMap<>();
        for (String line : lines) {
            int separator = line.indexOf('=');
            if (separator < 0) continue;
            block.put(line.substring(0, separator), line.substring(separator + 1));
            if (line.startsWith("progress=")) {
                blocks.add(block);
                block = new LinkedHashMap<>();
            }
        }
        return blocks;
    }

    /**
     * Parse a single completed {@code -progress} block into a {@link ProgressReport}.
     * Unknown or malformed fields fall back to defaults instead of failing: ffmpeg output
     * varies across versions (e.g. {@code out_time_us} versus legacy {@code out_time_ms}).
     *
     * @param block
     *         The block fields.
     * @param total
     *         The total media duration, used to compute the completion percentage.
     *
     * @return The parsed report.
     */
    static ProgressReport parseBlock(Map<String, String> block, Optional<Duration> total) {

        long outTimeUs = parseLong(block.get("out_time_us"), 0);
        if (outTimeUs == 0) {
            outTimeUs = parseLong(block.get("out_time_ms"), 0) * 1000;
        }
        if (outTimeUs == 0) {
            outTimeUs = parseTimestamp(block.get("out_time"));
        }

        return new ProgressReport(
                parseLong(block.get("frame"), 0),
                parseDouble(block.get("fps"), 0),
                outTimeUs,
                parseSpeed(block.get("speed")),
                parseLong(block.get("total_size"), 0),
                "end".equals(block.get("progress")),
                ProgressReport.percent(outTimeUs, total)
        );
    }

    private static long parseLong(String raw, long fallback) {

        if (raw == null) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDouble(String raw, double fallback) {

        if (raw == null) return fallback;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseSpeed(String raw) {

        if (raw == null) return 0;
        return parseDouble(raw.trim().endsWith("x") ? raw.trim().substring(0, raw.trim().length() - 1) : raw, 0);
    }

    private static long parseTimestamp(String raw) {

        if (raw == null) return 0;
        try {
            String[] parts = raw.trim().split(":");
            if (parts.length != 3) return 0;
            double seconds = Double.parseDouble(parts[2]);
            return (long) ((Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + seconds) * 1_000_000);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
