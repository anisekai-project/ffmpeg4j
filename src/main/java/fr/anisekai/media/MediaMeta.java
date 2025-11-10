package fr.anisekai.media;

import fr.anisekai.media.bin.FFMpeg;
import fr.anisekai.media.enums.CodecType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single media track (such as audio, video, or subtitle) used to reassemble a complete media file.
 * <p>
 * This class encapsulates the metadata of a media file, including its file path, optional name, language, and codec. It
 * is typically used as input for the {@link FFMpeg#combine(MediaMeta)} method to combine tracks into a unified media
 * container.
 */
public class MediaMeta {

    private final Path                path;
    private final CodecType           type;
    private final Map<String, String> metadata;
    private final List<String>        dispositions;

    /**
     * Create a {@link MediaMeta} instance for a given media file with optional metadata.
     *
     * @param path
     *         The media file.
     * @param type
     *         The {@link CodecType} for this {@link MediaMeta}.
     * @param name
     *         The name of the track.
     * @param language
     *         The language of the track.
     *
     * @throws IllegalArgumentException
     *         Threw if the codec cannot be determined from the file extension.
     */
    public MediaMeta(Path path, CodecType type, @Nullable String name, @Nullable String language) {

        this.path         = path.toAbsolutePath().normalize();
        this.type         = type;
        this.metadata     = new HashMap<>();
        this.dispositions = new ArrayList<>();

        if (name != null) {
            this.metadata.put("title", name);
        }

        if (language != null) {
            this.metadata.put("language", language);
        }
    }

    /**
     * Retrieve the media file associated with this track.
     *
     * @return The {@link Path} representing the media track.
     */
    public Path getPath() {

        return this.path;
    }

    /**
     * Retrieve the metadata associated to this {@link MediaMeta}.
     *
     * @return A map
     */
    public Map<String, String> getMetadata() {

        return this.metadata;
    }

    /**
     * Retrieve the disposition associated to this {@link MediaMeta}.
     *
     * @return A map
     */
    public List<String> getDispositions() {

        return this.dispositions;
    }

    /**
     * Retrieve the codec type for this media track.
     *
     * @return The {@link CodecType} used for this track.
     */
    public CodecType getCodecType() {

        return this.type;
    }

}
