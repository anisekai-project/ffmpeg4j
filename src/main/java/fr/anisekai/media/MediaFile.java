package fr.anisekai.media;

import fr.anisekai.media.bin.FFMpeg;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.CodecType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Represents a multimedia file composed of various media streams such as audio, video, or subtitles.
 * <p>
 * This class wraps a physical {@link File} and the set of {@link MediaStream} tracks detected within it, parsed via
 * ffmpeg.
 */
public final class MediaFile {

    /**
     * Parses a given file using ffmpeg and constructs a {@link MediaFile} from the detected streams.
     *
     * @param file
     *         The file to analyze.
     *
     * @return A {@link MediaFile} containing the parsed media streams.
     *
     * @throws IOException
     *         Threw if an I/O error occurs during probing or reading.
     * @throws InterruptedException
     *         Threw if the probing process is interrupted.
     */
    public static MediaFile of(Path file) throws IOException, InterruptedException {

        JSONObject json = FFMpeg.probe(file).intoTemporary().timeout(1, TimeUnit.MINUTES).run();
        return MediaFile.of(file, json);
    }

    /**
     * Construct a {@link MediaFile} from an already-parsed ffprobe document, without
     * spawning any process. Useful to test stream mapping against recorded fixtures.
     *
     * @param file
     *         The file the document was probed from.
     * @param probe
     *         The ffprobe JSON document, holding a {@code streams} array.
     *
     * @return A {@link MediaFile} containing the parsed media streams.
     *
     * @throws IOException
     *         Threw if the document holds an unsupported codec.
     */
    public static MediaFile of(Path file, JSONObject probe) throws IOException {

        Set<MediaStream> streams = new HashSet<>();

        JSONArray streamArray = probe.getJSONArray("streams");
        for (int i = 0; i < streamArray.length(); i++) {
            JSONObject streamData = streamArray.getJSONObject(i);

            CodecType type = CodecType.from(streamData.getString("codec_type"));
            if (type == null) continue; // Unsupported type of stream, just skip it.
            Codec codec = Codec.from(streamData.getString("codec_name"));
            if (codec == null) {
                throw new UnsupportedEncodingException("Unsupported codec: " + streamData.getString("codec_name"));
            }

            MediaStream stream = new MediaStream(codec, streamData);
            streams.add(stream);
        }

        return new MediaFile(file, streams, parseDuration(probe));
    }

    /**
     * Extract the media duration from a ffprobe document, when available. Some inputs
     * report no duration (or {@code "N/A"}), in which case an empty result is returned
     * instead of failing.
     *
     * @param probe
     *         The ffprobe JSON document.
     *
     * @return The media duration, or empty when unknown.
     */
    private static Duration parseDuration(JSONObject probe) {

        if (!probe.has("format")) {
            return null;
        }

        try {
            double seconds = Double.parseDouble(probe.getJSONObject("format").optString("duration", "N/A"));
            if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0) {
                return null;
            }
            return Duration.ofMillis((long) (seconds * 1000));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private final Path              path;
    private final List<MediaStream> streams;
    private final Duration          duration;

    private MediaFile(Path path, Collection<MediaStream> streams, Duration duration) {

        this.path     = path.toAbsolutePath().normalize();
        this.streams  = streams.stream()
                               .sorted(Comparator.comparingInt(MediaStream::getId))
                               .toList();
        this.duration = duration;
    }

    /**
     * Retrieve the physical file associated with this {@link MediaFile}.
     *
     * @return The underlying {@link Path}.
     */
    public Path getPath() {

        return this.path;
    }

    /**
     * Retrieve all {@link MediaStream} detected in the file.
     *
     * @return An unmodifiable {@link List} of {@link MediaStream} objects.
     */
    public List<MediaStream> getStreams() {

        return this.streams;
    }

    /**
     * Retrieve the media duration reported by ffprobe, when available.
     *
     * @return The media duration, or empty when the probed input reports none.
     */
    public Optional<Duration> getDuration() {

        return Optional.ofNullable(this.duration);
    }

    /**
     * Retrieve all {@link MediaStream} of the specified type (e.g., video, audio, subtitles).
     *
     * @param type
     *         The {@link CodecType} to filter by.
     *
     * @return An unmodifiable {@link List} of {@link MediaStream} matching the given type.
     */
    public List<MediaStream> getStreams(CodecType type) {

        return this.streams.stream().filter(stream -> stream.getCodec().getType() == type).toList();
    }

}
