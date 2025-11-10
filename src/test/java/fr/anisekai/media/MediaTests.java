package fr.anisekai.media;

import fr.anisekai.media.bin.FFMpeg;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.CodecType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DisplayName("Media (ffmpeg)")
@Tags({@Tag("slow-test"), @Tag("ffmpeg")})
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class MediaTests {

    public static final String TEST_DATA_MPD    = "data";
    public static final String TEST_DATA_FILE   = "video.mkv";
    public static final String TEST_OUTPUT_FILE = "output.mkv";

    public static final String TEST_DATA_RES_VIDEO   = "0.mp4";
    public static final String TEST_DATA_RES_AUDIO_1 = "1.ogg";
    public static final String TEST_DATA_RES_AUDIO_2 = "2.ogg";
    public static final String TEST_DATA_EXT_AUDIO_1 = "1.m4a";
    public static final String TEST_DATA_EXT_AUDIO_2 = "2.m4a";
    public static final String TEST_DATA_RES_SUBS_1  = "3.ass";
    public static final String TEST_DATA_RES_SUBS_2  = "4.ass";

    private static Path getTestFile(String name, boolean require) {

        Path dataDir = Path.of("src", "test", "resources");
        if (!Files.isDirectory(dataDir)) throw new RuntimeException("Unable to locate test data directory");
        Path path = dataDir.resolve(name);
        if (!Files.exists(path) && require) throw new RuntimeException("Unable to locate test data file " + name);
        return path;
    }

    @Test
    @DisplayName("ffprobe | Read MKV Data")
    public void testProbeFile() {

        Path      target = getTestFile(TEST_DATA_FILE, true);
        MediaFile media  = Assertions.assertDoesNotThrow(() -> MediaFile.of(target));

        Assertions.assertEquals(5, media.getStreams().size(), "Media stream count mismatch");
        Assertions.assertEquals(1, media.getStreams(CodecType.VIDEO).size(), "Video stream count mismatch");
        Assertions.assertEquals(2, media.getStreams(CodecType.AUDIO).size(), "Audio stream count mismatch");
        Assertions.assertEquals(2, media.getStreams(CodecType.SUBTITLE).size(), "Audio stream count mismatch");
    }

    @Test
    @DisplayName("ffmpeg | Codec passthrough")
    public void testPassthrough() {

        Path video  = getTestFile(TEST_DATA_RES_VIDEO, false);
        Path audio1 = getTestFile(TEST_DATA_EXT_AUDIO_1, false);
        Path audio2 = getTestFile(TEST_DATA_EXT_AUDIO_2, false);
        Path subs1  = getTestFile(TEST_DATA_RES_SUBS_1, false);
        Path subs2  = getTestFile(TEST_DATA_RES_SUBS_2, false);

        Path      target = getTestFile(TEST_DATA_FILE, true);
        MediaFile media  = Assertions.assertDoesNotThrow(() -> MediaFile.of(target));


        Map<MediaStream, Path> files = Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media)
                .copyVideo()
                .copyAudio()
                .copySubtitle()
                .into(target.getParent())
                .split()
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Assertions.assertEquals(5, files.size(), "File count mismatch");

        Assertions.assertTrue(Files.isRegularFile(video), "Video file mismatch");
        Assertions.assertTrue(Files.isRegularFile(audio1), "Audio(1) file mismatch");
        Assertions.assertTrue(Files.isRegularFile(audio2), "Audio(2) file mismatch");
        Assertions.assertTrue(Files.isRegularFile(subs1), "Subs(1) file mismatch");
        Assertions.assertTrue(Files.isRegularFile(subs2), "Subs(2) file mismatch");
    }

    @Test
    @DisplayName("ffmpeg | Create MPD")
    public void testMPD() throws IOException {

        Path target         = getTestFile(TEST_DATA_FILE, true);
        Path outputDir      = getTestFile(TEST_DATA_MPD, false);
        Path expectedOutput = outputDir.resolve("meta.mpd");

        MediaFile media = Assertions.assertDoesNotThrow(() -> MediaFile.of(target));

        Assertions.assertFalse(Files.isRegularFile(outputDir));

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        Path mdp = Assertions.assertDoesNotThrow(() -> FFMpeg
                .mdp(media)
                .into(outputDir)
                .as("meta.mpd")
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Assertions.assertNotNull(mdp);

        Assertions.assertTrue(Files.isRegularFile(mdp), "MPD metadata file does not exist");
        Assertions.assertTrue(Files.isRegularFile(expectedOutput), "MPD metadata not at the right output");
    }

    @Test
    @DisplayName("ffmpeg | MKV to split converted files")
    public void testExplode() {

        Path video  = getTestFile(TEST_DATA_RES_VIDEO, false);
        Path audio1 = getTestFile(TEST_DATA_RES_AUDIO_1, false);
        Path audio2 = getTestFile(TEST_DATA_RES_AUDIO_2, false);
        Path subs1  = getTestFile(TEST_DATA_RES_SUBS_1, false);
        Path subs2  = getTestFile(TEST_DATA_RES_SUBS_2, false);

        Path      target = getTestFile(TEST_DATA_FILE, true);
        MediaFile media  = Assertions.assertDoesNotThrow(() -> MediaFile.of(target));

        Map<MediaStream, Path> files = Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media)
                .video(Codec.H264)
                .audio(Codec.VORBIS)
                .copySubtitle()
                .into(target.getParent())
                .split()
                .timeout(2, TimeUnit.MINUTES)
                .run());

        Assertions.assertEquals(5, files.size(), "File count mismatch");

        Assertions.assertTrue(Files.isRegularFile(video), "Video file mismatch");
        Assertions.assertTrue(Files.isRegularFile(audio1), "Audio(1) file mismatch");
        Assertions.assertTrue(Files.isRegularFile(audio2), "Audio(2) file mismatch");
        Assertions.assertTrue(Files.isRegularFile(subs1), "Subs(1) file mismatch");
        Assertions.assertTrue(Files.isRegularFile(subs2), "Subs(2) file mismatch");
    }

    @Test
    @DisplayName("ffmpeg | Split files to MKV")
    public void testCombine() {

        Path target = getTestFile(TEST_DATA_FILE, true);
        Path output = getTestFile(TEST_OUTPUT_FILE, false);

        MediaFile media = Assertions.assertDoesNotThrow(() -> MediaFile.of(target));

        Map<MediaStream, Path> files = Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media)
                .copyVideo()
                .copyAudio()
                .copySubtitle()
                .into(output.getParent())
                .split()
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Path video  = getTestFile(TEST_DATA_RES_VIDEO, false);
        Path audio1 = getTestFile(TEST_DATA_EXT_AUDIO_1, false);
        Path audio2 = getTestFile(TEST_DATA_EXT_AUDIO_2, false);
        Path subs1  = getTestFile(TEST_DATA_RES_SUBS_1, false);
        Path subs2  = getTestFile(TEST_DATA_RES_SUBS_2, false);

        MediaMeta videoMeta  = new MediaMeta(video, CodecType.VIDEO, null, null);
        MediaMeta audio1Meta = new MediaMeta(audio1, CodecType.AUDIO, "Français", "fre");
        MediaMeta audio2Meta = new MediaMeta(audio2, CodecType.AUDIO, "日本語", "jpn");
        MediaMeta subs1Meta  = new MediaMeta(subs1, CodecType.SUBTITLE, "Français (Forced)", "fre");
        MediaMeta subs2Meta  = new MediaMeta(subs2, CodecType.SUBTITLE, "Français", "fre");


        Path result = Assertions.assertDoesNotThrow(() -> FFMpeg
                .combine(videoMeta)
                .with(audio1Meta)
                .with(audio2Meta)
                .with(subs1Meta)
                .with(subs2Meta)
                .file(output)
                .timeout(1, TimeUnit.MINUTES)
                .run()
        );
        MediaFile merged = Assertions.assertDoesNotThrow(() -> MediaFile.of(result));

        Assertions.assertEquals(5, merged.getStreams().size(), "Media stream count mismatch");
        Assertions.assertEquals(1, merged.getStreams(CodecType.VIDEO).size(), "Video stream count mismatch");
        Assertions.assertEquals(2, merged.getStreams(CodecType.AUDIO).size(), "Audio stream count mismatch");
        Assertions.assertEquals(2, merged.getStreams(CodecType.SUBTITLE).size(), "Audio stream count mismatch");
    }

    @Test
    @DisplayName("ffmpeg | Codec passthrough + ignore")
    public void testPassthroughWithIgnore() {

        Path video  = getTestFile(TEST_DATA_RES_VIDEO, false);
        Path audio1 = getTestFile(TEST_DATA_EXT_AUDIO_1, false);
        Path audio2 = getTestFile(TEST_DATA_EXT_AUDIO_2, false);
        Path subs1  = getTestFile(TEST_DATA_RES_SUBS_1, false);
        Path subs2  = getTestFile(TEST_DATA_RES_SUBS_2, false);

        Path      target = getTestFile(TEST_DATA_FILE, true);
        MediaFile media  = Assertions.assertDoesNotThrow(() -> MediaFile.of(target));

        Map<MediaStream, Path> files = Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media)
                .noVideo()
                .copyAudio()
                .noSubtitle()
                .into(target.getParent())
                .split()
                .timeout(1, TimeUnit.MINUTES)
                .run());

        Assertions.assertEquals(2, files.size(), "File count mismatch");

        Assertions.assertFalse(Files.isRegularFile(video), "Video file mismatch");
        Assertions.assertTrue(Files.isRegularFile(audio1), "Audio(1) file mismatch");
        Assertions.assertTrue(Files.isRegularFile(audio2), "Audio(2) file mismatch");
        Assertions.assertFalse(Files.isRegularFile(subs1), "Subs(1) file mismatch");
        Assertions.assertFalse(Files.isRegularFile(subs2), "Subs(2) file mismatch");
    }

    @Test
    @DisplayName("ffprobe | Simple conversion (without subs)")
    public void testSimpleConvert() {

        Path target = getTestFile(TEST_DATA_FILE, true);
        Path output = getTestFile(TEST_OUTPUT_FILE, false);

        MediaFile media = Assertions.assertDoesNotThrow(() -> MediaFile.of(target));

        Assertions.assertDoesNotThrow(() -> FFMpeg
                .convert(media)
                .video(Codec.H264)
                .audio(Codec.AAC)
                .noSubtitle()
                .file(output)
                .timeout(2, TimeUnit.MINUTES)
                .run()
        );

        MediaFile outputMedia = Assertions.assertDoesNotThrow(() -> MediaFile.of(output));
        Assertions.assertEquals(3, outputMedia.getStreams().size(), "Streams count mismatch");

        for (MediaStream stream : outputMedia.getStreams()) {
            switch (stream.getCodec().getType()) {
                case VIDEO -> Assertions.assertEquals(Codec.H264, stream.getCodec(), "Codec mismatch");
                case AUDIO -> Assertions.assertEquals(Codec.AAC, stream.getCodec(), "Codec mismatch");
            }
        }
    }

    @AfterEach
    public void onTestFinished() throws IOException {

        Path output    = getTestFile(TEST_OUTPUT_FILE, false);
        Path video     = getTestFile(TEST_DATA_RES_VIDEO, false);
        Path resAudio1 = getTestFile(TEST_DATA_RES_AUDIO_1, false);
        Path resAudio2 = getTestFile(TEST_DATA_RES_AUDIO_2, false);
        Path extAudio1 = getTestFile(TEST_DATA_EXT_AUDIO_1, false);
        Path extAudio2 = getTestFile(TEST_DATA_EXT_AUDIO_2, false);
        Path subs1     = getTestFile(TEST_DATA_RES_SUBS_1, false);
        Path subs2     = getTestFile(TEST_DATA_RES_SUBS_2, false);
        Path mpd       = getTestFile(TEST_DATA_MPD, false);

        delete(output);
        delete(video);
        delete(resAudio1);
        delete(resAudio2);
        delete(extAudio1);
        delete(extAudio2);
        delete(subs1);
        delete(subs2);
        delete(mpd);
    }

    private static void delete(Path path) throws IOException {

        if (!Files.exists(path)) {
            return;
        }

        if (Files.isRegularFile(path)) {
            Files.delete(path);
            return;
        }

        if (Files.isDirectory(path)) {
            Files.walkFileTree(
                    path,
                    new SimpleFileVisitor<>() {

                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {

                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, @Nullable IOException exc) throws IOException {

                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
            return;
        }

        throw new UnsupportedOperationException("Unable to delete path: " + path);
    }

}
