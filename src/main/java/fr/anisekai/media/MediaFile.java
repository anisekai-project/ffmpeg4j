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

        Set<MediaStream> streams = new HashSet<>();

        JSONArray streamArray = json.getJSONArray("streams");
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

        return new MediaFile(file, streams);
    }

    private final Path              path;
    private final List<MediaStream> streams;

    private MediaFile(Path path, Collection<MediaStream> streams) {

        this.path    = path.toAbsolutePath().normalize();
        this.streams = streams.stream()
                              .sorted(Comparator.comparingInt(MediaStream::getId))
                              .toList();
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
