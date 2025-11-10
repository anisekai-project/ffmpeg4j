package fr.anisekai.media.enums;

import org.jetbrains.annotations.Nullable;

/**
 * Represents the type of codec: video, audio, or subtitle.
 * <p>
 * Each type is associated with a character used by ffmpeg to identify stream types.
 */
public enum CodecType {

    /**
     * Codec type used for video tracks.
     */
    VIDEO,

    /**
     * Codec type used for audio tracks.
     */
    AUDIO,

    /**
     * Codec type used for subtitle tracks.
     */
    SUBTITLE;

    /**
     * Returns the character used by ffmpeg to identify this codec type.
     *
     * @return The character representing this codec type
     */
    public char getChar() {

        return switch (this) {
            case VIDEO -> 'v';
            case AUDIO -> 'a';
            case SUBTITLE -> 's';
        };
    }

    /**
     * Attempts to resolve a {@link CodecType} from a given string.
     *
     * @param codecTypeName
     *         The name to match (case-insensitive)
     *
     * @return The matching {@link CodecType}, or {@code null} if no match is found
     */
    public static @Nullable CodecType from(String codecTypeName) {

        for (CodecType type : CodecType.values()) {
            if (type.name().equalsIgnoreCase(codecTypeName)) {
                return type;
            }
        }
        return null;
    }
}
