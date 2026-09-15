package fr.anisekai.media.bin;

import fr.anisekai.media.MediaFile;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.interfaces.ProgressReport;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@DisplayName("Progress (convert integration via stub)")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ProgressReportingTests {

    private static final Path RESOURCES = Path.of("src", "test", "resources").toAbsolutePath();

    private static void writeProgressStub(Path dir, Path argvLog) throws Exception {

        Path stub = dir.resolve("ffmpeg");
        Files.writeString(stub, """
                #!/bin/sh
                echo "$@" >> '%s'
                prog=""
                prev=""
                last=""
                for arg in "$@"; do
                  if [ "$prev" = "-progress" ]; then prog="$arg"; fi
                  prev="$arg"
                  last="$arg"
                done
                i=1
                while [ $i -le 3 ]; do
                  printf 'frame=%%s\\nfps=25.0\\nout_time_us=%%s\\nspeed=1.0x\\ntotal_size=1000\\nprogress=continue\\n' "$i" "$((i * 40000))" >> "$prog"
                  sleep 0.3
                  i=$((i + 1))
                done
                printf 'frame=4\\nfps=25.0\\nout_time_us=160000\\nspeed=1.0x\\ntotal_size=2000\\nprogress=end\\n' >> "$prog"
                : > "$last"
                """.formatted(argvLog.toString()));
        Files.setPosixFilePermissions(stub, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private static MediaFile media(Path input) throws Exception {

        JSONObject probe = new JSONObject(Files.readString(RESOURCES.resolve("probe.json")));
        probe.put("format", new JSONObject().put("duration", "2.0"));
        return MediaFile.of(input, probe);
    }

    @AfterEach
    public void clearBinaryOverrides() {

        System.clearProperty("ffmpeg.binary");
    }

    @Test
    @DisplayName("convert | Reports progress with percentages and cleans up")
    public void testConvertReportsProgress(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeProgressStub(root, argvLog);
        System.setProperty("ffmpeg.binary", root.resolve("ffmpeg").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);
        Path output = root.resolve("output.mkv");

        List<ProgressReport> reports = Collections.synchronizedList(new ArrayList<>());
        Path result = Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media(input))
                .video(Codec.H264)
                .audio(Codec.AAC)
                .copySubtitle()
                .progressListener(reports::add)
                .file(output)
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Assertions.assertEquals(output.toAbsolutePath().normalize(), result, "Output mismatch");

        // The -progress argument sits right after the input, before any output.
        List<String> argv = List.of(Files.readString(argvLog).trim().split(" "));
        int progress = argv.indexOf("-progress");
        Assertions.assertEquals(argv.indexOf("-i") + 2, progress, "Progress argument mismatch");

        Assertions.assertTrue(reports.size() >= 2, "Report count mismatch: " + reports.size());
        ProgressReport last = reports.getLast();
        Assertions.assertTrue(last.finished(), "Finished mismatch");
        Assertions.assertEquals(8.0, last.percent().orElseThrow(), 0.0001, "Percent mismatch");

        double previous = -1;
        for (ProgressReport report : reports) {
            double percent = report.percent().orElseThrow();
            Assertions.assertTrue(percent >= previous, "Percent must not decrease");
            previous = percent;
        }

        try (Stream<Path> tmp = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            Assertions.assertTrue(
                    tmp.noneMatch(path -> path.getFileName().toString().startsWith("ffmpeg-progress-")),
                    "Progress file must be deleted");
        }
    }

    @Test
    @DisplayName("convert | Reports without percentage when duration is unknown")
    public void testConvertWithoutDuration(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeProgressStub(root, argvLog);
        System.setProperty("ffmpeg.binary", root.resolve("ffmpeg").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);

        JSONObject probe = new JSONObject(Files.readString(RESOURCES.resolve("probe.json")));
        MediaFile unknown = MediaFile.of(input, probe);
        Assertions.assertTrue(unknown.getDuration().isEmpty(), "Duration mismatch");

        List<ProgressReport> reports = Collections.synchronizedList(new ArrayList<>());
        Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(unknown)
                .copyVideo()
                .copyAudio()
                .copySubtitle()
                .progressListener(reports::add)
                .file(root.resolve("output.mkv"))
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Instant deadline = Instant.now().plusSeconds(5);
        while (reports.isEmpty() && Instant.now().isBefore(deadline)) {
            Thread.sleep(50);
        }
        Assertions.assertFalse(reports.isEmpty(), "Report count mismatch");
        Assertions.assertTrue(reports.getLast().percent().isEmpty(), "Percent mismatch");
    }
}
