package fr.anisekai.media.bin;

import fr.anisekai.media.interfaces.ProgressListener;
import fr.anisekai.media.interfaces.ProgressReport;
import fr.anisekai.media.bin.wrapper.tasks.ProgressMonitor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

@DisplayName("Progress (monitor and reports)")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ProgressMonitorTests {

    private static final String BLOCK = """
            frame=%s
            fps=25.0
            stream_0_0_q=-0.0
            bitrate=100.0kbits/s
            total_size=1000
            out_time_us=%s
            out_time=00:00:00.000000
            dup_frames=0
            drop_frames=0
            speed=1.0x
            progress=%s
            """;

    private static List<ProgressReport> awaitReports(List<ProgressReport> reports, int expected) throws Exception {

        Instant deadline = Instant.now().plusSeconds(5);
        while (reports.size() < expected && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
        }
        Assertions.assertEquals(expected, reports.size(), "Report count mismatch");
        return reports;
    }

    @Test
    @DisplayName("monitor | Forwards parsed blocks with percentages")
    public void testMonitorForwardsBlocks(@TempDir Path root) throws Exception {

        Path file = root.resolve("progress.log");
        Files.writeString(file, BLOCK.formatted(10, 40000, "continue") + BLOCK.formatted(20, 80000, "continue")
                + BLOCK.formatted(30, 2000000, "end"));

        List<ProgressReport> reports = Collections.synchronizedList(new ArrayList<>());
        ProgressMonitor monitor = new ProgressMonitor(file, Optional.of(Duration.ofSeconds(40)), reports::add);
        monitor.start();
        try {
            awaitReports(reports, 3);
        } finally {
            monitor.close();
        }

        ProgressReport first = reports.get(0);
        Assertions.assertEquals(10, first.frame(), "Frame mismatch");
        Assertions.assertEquals(25.0, first.fps(), "Fps mismatch");
        Assertions.assertEquals(40000, first.outTimeUs(), "Timestamp mismatch");
        Assertions.assertEquals(1.0, first.speed(), "Speed mismatch");
        Assertions.assertEquals(1000, first.totalSize(), "Size mismatch");
        Assertions.assertFalse(first.finished(), "Finished mismatch");
        Assertions.assertEquals(0.1, first.percent().orElseThrow(), 0.0001, "Percent mismatch");

        ProgressReport last = reports.get(2);
        Assertions.assertTrue(last.finished(), "Finished mismatch");
        Assertions.assertEquals(5.0, last.percent().orElseThrow(), 0.0001, "Percent mismatch");
    }

    @Test
    @DisplayName("monitor | Supports legacy out_time_ms")
    public void testLegacyTimestamp(@TempDir Path root) throws Exception {

        Path file = root.resolve("progress.log");
        Files.writeString(file, "frame=5\nout_time_ms=1500\nprogress=end\n");

        List<ProgressReport> reports = Collections.synchronizedList(new ArrayList<>());
        ProgressMonitor monitor = new ProgressMonitor(file, Optional.empty(), reports::add);
        monitor.start();
        try {
            awaitReports(reports, 1);
        } finally {
            monitor.close();
        }

        Assertions.assertEquals(1500000, reports.getFirst().outTimeUs(), "Timestamp mismatch");
        Assertions.assertTrue(reports.getFirst().percent().isEmpty(), "Percent mismatch");
    }

    @Test
    @DisplayName("monitor | Ignores partial blocks and malformed lines")
    public void testPartialAndMalformed(@TempDir Path root) throws Exception {

        Path file = root.resolve("progress.log");
        Files.writeString(file, "frame=bogus\nno-separator-here\nout_time_us=also-bogus\n");

        List<ProgressReport> reports = Collections.synchronizedList(new ArrayList<>());
        ProgressMonitor monitor = new ProgressMonitor(file, Optional.empty(), reports::add);
        monitor.start();
        Thread.sleep(600);
        monitor.close();

        Assertions.assertTrue(reports.isEmpty(), "Partial blocks must not be reported");
    }

    @Test
    @DisplayName("monitor | A failing listener stops monitoring silently")
    public void testFailingListener(@TempDir Path root) throws Exception {

        Path file = root.resolve("progress.log");
        Files.writeString(file, BLOCK.formatted(1, 1000, "continue"));

        AtomicInteger invocations = new AtomicInteger();
        ProgressListener failing = report -> {
            invocations.incrementAndGet();
            throw new IllegalStateException("listener boom");
        };
        ProgressMonitor monitor = new ProgressMonitor(file, Optional.empty(), failing);
        monitor.start();
        Thread.sleep(600);
        monitor.close();

        Assertions.assertEquals(1, invocations.get(), "Monitoring must stop after a listener failure");
    }

    @Test
    @DisplayName("report | Computes and clamps percentages")
    public void testPercent() {

        Assertions.assertEquals(50.0,
                ProgressReport.percent(1000000, Optional.of(Duration.ofSeconds(2))).orElseThrow(),
                0.0001, "Percent mismatch");
        Assertions.assertEquals(100.0,
                ProgressReport.percent(5000000, Optional.of(Duration.ofSeconds(2))).orElseThrow(),
                0.0001, "Percent mismatch");
        Assertions.assertEquals(OptionalDouble.empty(),
                ProgressReport.percent(1000, Optional.empty()), "Percent mismatch");
        Assertions.assertEquals(OptionalDouble.empty(),
                ProgressReport.percent(1000, Optional.of(Duration.ZERO)), "Percent mismatch");
        Assertions.assertEquals(OptionalDouble.empty(),
                ProgressReport.percent(-1, Optional.of(Duration.ofSeconds(2))), "Percent mismatch");
    }
}
