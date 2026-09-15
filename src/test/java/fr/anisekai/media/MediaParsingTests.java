package fr.anisekai.media;

import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.media.enums.Disposition;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@DisplayName("MediaFile (probe parsing)")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class MediaParsingTests {

    private static final Path RESOURCES = Path.of("src", "test", "resources").toAbsolutePath();

    private static JSONObject probeFixture() throws Exception {

        return new JSONObject(Files.readString(RESOURCES.resolve("probe.json")));
    }

    @Test
    @DisplayName("probe | Maps streams, skips unknown types, sorts by id")
    public void testParseFixture() throws Exception {

        MediaFile media = Assertions.assertDoesNotThrow(
                () -> MediaFile.of(Path.of("fixture.mkv"), probeFixture()));

        // The attachment stream (index 5) is skipped, the rest is sorted by id
        // even though the fixture lists them out of order.
        List<MediaStream> streams = media.getStreams();
        Assertions.assertEquals(5, streams.size(), "Stream count mismatch");
        Assertions.assertEquals(List.of(0, 1, 2, 3, 4),
                streams.stream().map(MediaStream::getId).toList(), "Stream order mismatch");

        Assertions.assertEquals(1, media.getStreams(CodecType.VIDEO).size(), "Video stream count mismatch");
        Assertions.assertEquals(2, media.getStreams(CodecType.AUDIO).size(), "Audio stream count mismatch");
        Assertions.assertEquals(2, media.getStreams(CodecType.SUBTITLE).size(), "Subtitle stream count mismatch");

        Assertions.assertEquals(Codec.H264, streams.get(0).getCodec(), "Codec mismatch");
        Assertions.assertEquals(Codec.AAC, streams.get(1).getCodec(), "Codec mismatch");
        Assertions.assertEquals(Codec.AC3, streams.get(2).getCodec(), "Codec mismatch");
        Assertions.assertEquals(Codec.SUBRIP, streams.get(3).getCodec(), "Codec mismatch");
        Assertions.assertEquals(Codec.ASS, streams.get(4).getCodec(), "Codec mismatch");
    }

    @Test
    @DisplayName("probe | Maps dispositions and lowercases metadata")
    public void testDispositionsAndMetadata() throws Exception {

        MediaFile media = MediaFile.of(Path.of("fixture.mkv"), probeFixture());
        List<MediaStream> streams = media.getStreams();

        Assertions.assertTrue(streams.get(0).getDispositions().contains(Disposition.DEFAULT), "Disposition mismatch");
        Assertions.assertTrue(streams.get(3).getDispositions().contains(Disposition.FORCED), "Disposition mismatch");
        Assertions.assertEquals("Main", streams.get(0).getMetadata().get("title"), "Metadata mismatch");
        Assertions.assertEquals("fre", streams.get(2).getMetadata().get("language"), "Metadata mismatch");
    }

    @Test
    @DisplayName("probe | Lowercases metadata keys")
    public void testMetadataKeysLowercased() throws Exception {

        JSONObject stream = new JSONObject()
                .put("index", 0)
                .put("codec_name", "h264")
                .put("codec_type", "video")
                .put("disposition", new JSONObject())
                .put("tags", new JSONObject().put("Title", "Upper").put("LANGUAGE", "eng"));
        JSONObject probe = new JSONObject().put("streams", new JSONArray().put(stream));

        MediaFile media = MediaFile.of(Path.of("fixture.mkv"), probe);

        Assertions.assertEquals("Upper", media.getStreams().getFirst().getMetadata().get("title"), "Metadata mismatch");
        Assertions.assertEquals("eng", media.getStreams().getFirst().getMetadata().get("language"), "Metadata mismatch");
    }

    @Test
    @DisplayName("probe | Rejects unsupported codecs")
    public void testUnsupportedCodec() throws Exception {

        JSONObject stream = new JSONObject()
                .put("index", 0)
                .put("codec_name", "fake_codec_xyz")
                .put("codec_type", "video")
                .put("disposition", new JSONObject())
                .put("tags", new JSONObject());
        JSONObject probe = new JSONObject().put("streams", new JSONArray().put(stream));

        Assertions.assertThrows(
                UnsupportedEncodingException.class,
                () -> MediaFile.of(Path.of("fixture.mkv"), probe));
    }
}
