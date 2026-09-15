package fr.anisekai.media.bin;

import fr.anisekai.media.MediaFile;
import fr.anisekai.media.MediaMeta;
import fr.anisekai.media.MediaStream;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.media.interfaces.MediaStreamMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DisplayName("Commands (argument assembly via stubs)")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class CommandLineTests {

    private static final Path RESOURCES = Path.of("src", "test", "resources").toAbsolutePath();

    private static void writeExecutable(Path path, String body) throws Exception {

        Files.writeString(path, body);
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private static void writeFfmpegStub(Path dir, Path argvLog, Path cannedOutput) throws Exception {

        String copy = cannedOutput == null
                ? ": > \"$last\""
                : "cp '" + cannedOutput.toString() + "' \"$last\"";
        writeExecutable(dir.resolve("ffmpeg"), """
                #!/bin/sh
                echo "$@" >> '%s'
                last=""
                for arg in "$@"; do last="$arg"; done
                %s
                """.formatted(argvLog.toString(), copy));
    }

    private static void writeFfprobeStub(Path dir, Path argvLog, Path cannedProbe) throws Exception {

        writeExecutable(dir.resolve("ffprobe"), """
                #!/bin/sh
                echo "$@" >> '%s'
                out=""
                prev=""
                for arg in "$@"; do
                  if [ "$prev" = "-o" ]; then out="$arg"; fi
                  prev="$arg"
                done
                cp '%s' "$out"
                """.formatted(argvLog.toString(), cannedProbe.toString()));
    }

    private static List<String> recordedArgv(Path argvLog) throws Exception {

        return List.of(Files.readString(argvLog).trim().split(" "));
    }

    private static MediaFile media(Path input) throws Exception {

        JSONObject probe = new JSONObject(Files.readString(RESOURCES.resolve("probe.json")));
        return MediaFile.of(input, probe);
    }

    @AfterEach
    public void clearBinaryOverrides() {

        System.clearProperty("ffmpeg.binary");
        System.clearProperty("ffprobe.binary");
    }

    @Test
    @DisplayName("ffprobe | Records exact arguments and returns the document")
    public void testProbeArguments(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeFfprobeStub(root, argvLog, RESOURCES.resolve("probe.json"));
        System.setProperty("ffprobe.binary", root.resolve("ffprobe").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);

        JSONObject json = Assertions.assertDoesNotThrow(
                () -> FFMpeg.probe(input).intoTemporary().timeout(1, TimeUnit.MINUTES).run());

        Assertions.assertEquals(6, json.getJSONArray("streams").length(), "Stream count mismatch");

        List<String> argv = recordedArgv(argvLog);
        Assertions.assertEquals(7, argv.size(), "Argument count mismatch");
        Assertions.assertEquals(
                List.of("-show_streams", "-of", "json", "-i", input.toString(), "-o"),
                argv.subList(0, 6), "Argument mismatch");
    }

    @Test
    @DisplayName("ffprobe | MediaFile wires probing to parsing without a binary")
    public void testProbeToMediaFile(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeFfprobeStub(root, argvLog, RESOURCES.resolve("probe.json"));
        System.setProperty("ffprobe.binary", root.resolve("ffprobe").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);

        MediaFile file = Assertions.assertDoesNotThrow(() -> MediaFile.of(input));

        Assertions.assertEquals(5, file.getStreams().size(), "Stream count mismatch");
    }

    @Test
    @DisplayName("ffmpeg | Convert records the exact mapping arguments")
    public void testConvertArguments(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeFfmpegStub(root, argvLog, null);
        System.setProperty("ffmpeg.binary", root.resolve("ffmpeg").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);
        Path output = root.resolve("output.mkv");

        Path result = Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media(input))
                .video(Codec.H264)
                .audio(Codec.AAC)
                .copySubtitle()
                .file(output)
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Assertions.assertEquals(output.toAbsolutePath().normalize(), result, "Output mismatch");

        String in = media(input).getPath().toString();
        Assertions.assertEquals(
                List.of("-i", in,
                        "-map", "0:0", "-c:v", "libx264", "-crf", "25", "-vf", "format=yuv420p",
                        "-map", "0:1", "-c:a", "aac",
                        "-map", "0:2", "-c:a", "aac",
                        "-map", "0:3", "-c:s", "copy",
                        "-map", "0:4", "-c:s", "copy",
                        output.toAbsolutePath().normalize().toString()),
                recordedArgv(argvLog), "Argument mismatch");
    }

    @Test
    @DisplayName("ffmpeg | Copy codecs skip encoding arguments")
    public void testConvertCopySkipsEncodingArguments(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeFfmpegStub(root, argvLog, null);
        System.setProperty("ffmpeg.binary", root.resolve("ffmpeg").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);
        Path output = root.resolve("output.mkv");

        Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media(input))
                .copyVideo()
                .copyAudio()
                .copySubtitle()
                .file(output)
                .timeout(1, TimeUnit.MINUTES)
                .run());

        List<String> argv = recordedArgv(argvLog);
        Assertions.assertTrue(argv.containsAll(List.of("-c:v", "copy", "-c:a", "copy", "-c:s", "copy")),
                "Copy arguments mismatch: " + argv);
        Assertions.assertFalse(argv.contains("-crf"), "Encoding arguments must be skipped: " + argv);
        Assertions.assertFalse(argv.contains("-vf"), "Encoding arguments must be skipped: " + argv);
    }

    @Test
    @DisplayName("ffmpeg | Disabled streams are not mapped")
    public void testConvertDisabledStreams(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeFfmpegStub(root, argvLog, null);
        System.setProperty("ffmpeg.binary", root.resolve("ffmpeg").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);
        Path output = root.resolve("output.mkv");

        Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media(input))
                .noVideo()
                .copyAudio()
                .noSubtitle()
                .file(output)
                .timeout(1, TimeUnit.MINUTES)
                .run());

        String in = media(input).getPath().toString();
        Assertions.assertEquals(
                List.of("-i", in,
                        "-map", "0:1", "-c:a", "copy",
                        "-map", "0:2", "-c:a", "copy",
                        output.toAbsolutePath().normalize().toString()),
                recordedArgv(argvLog), "Argument mismatch");
    }

    @Test
    @DisplayName("ffmpeg | Split names one file per stream")
    public void testSplitArguments(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeFfmpegStub(root, argvLog, null);
        System.setProperty("ffmpeg.binary", root.resolve("ffmpeg").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);

        Map<MediaStream, Path> files = Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media(input))
                .copyVideo()
                .copyAudio()
                .copySubtitle()
                .into(root)
                .split()
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Assertions.assertEquals(
                List.of("0.mp4", "1.m4a", "2.ac3", "3.srt", "4.ass"),
                files.values().stream().map(path -> path.getFileName().toString()).sorted().toList(),
                "Split filenames mismatch");

        String in = media(input).getPath().toString();
        Assertions.assertEquals(
                List.of("-i", in,
                        "-map", "0:0", "-c:v", "copy", "0.mp4",
                        "-map", "0:1", "-c:a", "copy", "1.m4a",
                        "-map", "0:2", "-c:a", "copy", "2.ac3",
                        "-map", "0:3", "-c:s", "copy", "3.srt",
                        "-map", "0:4", "-c:s", "copy", "4.ass"),
                recordedArgv(argvLog), "Argument mismatch");
    }

    @Test
    @DisplayName("ffmpeg | Combine records inputs, maps and metadata")
    public void testCombineArguments(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        writeFfmpegStub(root, argvLog, null);
        System.setProperty("ffmpeg.binary", root.resolve("ffmpeg").toString());

        Path video = root.resolve("video.mp4");
        Path audio = root.resolve("audio.m4a");
        Path output = root.resolve("output.mkv");

        Path result = Assertions.assertDoesNotThrow(() -> FFMpeg
                .combine(new MediaMeta(video, CodecType.VIDEO, "Main", null))
                .with(new MediaMeta(audio, CodecType.AUDIO, "Stereo", null))
                .file(output)
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Assertions.assertEquals(output.toAbsolutePath().normalize(), result, "Output mismatch");
        Assertions.assertEquals(
                List.of("-i", video.toAbsolutePath().normalize().toString(),
                        "-i", audio.toAbsolutePath().normalize().toString(),
                        "-map", "0:v", "-metadata:s:0", "title=Main",
                        "-map", "1:a", "-metadata:s:1", "title=Stereo",
                        "-c", "copy",
                        output.toAbsolutePath().normalize().toString()),
                recordedArgv(argvLog), "Argument mismatch");
    }

    @Test
    @DisplayName("ffmpeg | MPD records maps and patches labels")
    public void testMpdArguments(@TempDir Path root) throws Exception {

        Path argvLog = root.resolve("argv.log");
        Path mpdDir = root.resolve("mpd");
        Files.createDirectory(mpdDir);
        writeFfmpegStub(root, argvLog, RESOURCES.resolve("stub.mpd"));
        System.setProperty("ffmpeg.binary", root.resolve("ffmpeg").toString());

        Path input = root.resolve("input.mkv");
        Files.createFile(input);

        Path result = Assertions.assertDoesNotThrow(() -> FFMpeg
                .mdp(media(input))
                .into(mpdDir)
                .as("meta.mpd")
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Path mdp = mpdDir.toAbsolutePath().normalize().resolve("meta.mpd");
        Assertions.assertEquals(mdp, result, "Output mismatch");

        String in = media(input).getPath().toString();
        Assertions.assertEquals(
                "-i " + in
                        + " -map 0:0 -map 0:1 -map 0:2"
                        + " -c copy"
                        + " -adaptation_sets id=0,streams=0 id=1,streams=1 id=2,streams=2"
                        + " " + mdp,
                Files.readString(argvLog).trim(), "Argument mismatch");

        String patched = Files.readString(mdp);
        Assertions.assertTrue(patched.contains("label=\"Main\""), "Video label mismatch");
        Assertions.assertTrue(patched.contains("label=\"Stereo\""), "Audio label mismatch");
    }

    @Test
    @DisplayName("mapper | Attached pictures are skipped")
    public void testAttachedPictureSkipped() {

        JSONObject stream = new JSONObject()
                .put("index", 9)
                .put("disposition", new JSONObject().put("attached_pic", 1))
                .put("tags", new JSONObject());
        MediaStream attached = new MediaStream(Codec.MJPEG, stream);

        RecordingBinary binary = new RecordingBinary();
        binary.recorded().clear(); // Drop the executable name recorded by the super constructor.
        MediaStreamMapper.DEFAULT.map(binary, attached, Codec.MJPEG);

        Assertions.assertTrue(binary.recorded().isEmpty(), "Attached picture must be skipped");

        JSONObject video = new JSONObject()
                .put("index", 0)
                .put("disposition", new JSONObject())
                .put("tags", new JSONObject());
        RecordingBinary control = new RecordingBinary();
        control.recorded().clear(); // Drop the executable name recorded by the super constructor.
        MediaStreamMapper.DEFAULT.map(control, new MediaStream(Codec.H264, video), Codec.H264);

        Assertions.assertEquals(
                List.of("-map", "0:0", "-c:v", "libx264", "-crf", "25", "-vf", "format=yuv420p"),
                control.recorded(), "Argument mismatch");
    }

    @Test
    @DisplayName("builders | Reject mismatched codecs and filenames")
    public void testBuilderValidation(@TempDir Path root) throws Exception {

        Path input = root.resolve("input.mkv");
        Files.createFile(input);
        MediaFile file = media(input);

        Assertions.assertThrows(IllegalArgumentException.class, () -> FFMpeg.convert(file).video(Codec.AAC));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FFMpeg.convert(file).audio(Codec.H264));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FFMpeg.convert(file).subtitle(Codec.AAC));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FFMpeg.mdp(file).into(root).as("meta.txt"));
    }

    private static final class RecordingBinary extends Binary {

        // Lazily initialized: the super constructor records the executable
        // through addArgument before subclass fields are assigned.
        private List<String> recorded;

        private RecordingBinary() {

            super("recording");
        }

        @Override
        public void addArgument(Object argument) {

            if (this.recorded == null) {
                this.recorded = new ArrayList<>();
            }
            this.recorded.add(argument.toString());
        }

        @Override
        public void addArguments(Object... args) {

            for (Object argument : args) {
                this.recorded.add(argument.toString());
            }
        }

        private List<String> recorded() {

            return this.recorded;
        }
    }
}
