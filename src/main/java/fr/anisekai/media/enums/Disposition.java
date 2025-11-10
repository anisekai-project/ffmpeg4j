package fr.anisekai.media.enums;

import fr.anisekai.media.MediaStream;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Enum for the most common {@link MediaStream} dispositions.
 */
public enum Disposition {

    /**
     * Indicates that the stream is used by default in the media.
     */
    DEFAULT,

    /**
     * Indicates that the stream is a dubbed version.
     */
    DUB,

    /**
     * Indicates that the stream is in the original media language.
     */
    ORIGINAL,

    /**
     * Indicates that the stream is a commentary.
     */
    COMMENT,

    /**
     * Indicates a stream with lyrics.
     */
    LYRICS,

    /**
     * Indicates a karaoke version of the stream.
     */
    KARAOKE,

    /**
     * Indicates a subtitle stream meant to always be displayed.
     */
    FORCED,

    /**
     * Indicates a stream tailored for the hearing impaired.
     */
    HEARING_IMPAIRED,

    /**
     * Indicates a stream tailored for the visually impaired.
     */
    VISUAL_IMPAIRED,

    /**
     * Indicates that the stream contains clean effects (no dialogue).
     */
    CLEAN_EFFECTS,

    /**
     * Indicates that the stream is an attached picture (e.g., cover art).
     */
    ATTACHED_PIC,

    /**
     * Indicates that the stream contains timed thumbnails.
     */
    TIMED_THUMBNAILS,

    /**
     * Indicates that the stream contains captions.
     */
    CAPTIONS,

    /**
     * Indicates that the stream contains descriptions.
     */
    DESCRIPTIONS,

    /**
     * Indicates that the stream contains metadata.
     */
    METADATA,

    /**
     * Indicates that the stream is dependent on another stream.
     */
    DEPENDENT,

    /**
     * Indicates that the stream is a still image.
     */
    STILL_IMAGE;

    private final int bit;

    Disposition() {

        this.bit = 1 << this.ordinal();
    }

    /**
     * Attempts to resolve a {@link Disposition} from a given string.
     *
     * @param name
     *         The name to match (case-insensitive)
     *
     * @return The matching {@link Disposition}, or {@code null}.
     */
    public static @Nullable Disposition from(String name) {

        for (Disposition disposition : Disposition.values()) {
            if (disposition.name().equalsIgnoreCase(name)) {
                return disposition;
            }
        }
        return null;
    }

    /**
     * Retrieve a collection of {@link Disposition} defined by the provided bits.
     *
     * @param bits
     *         The bit value
     *
     * @return A collection of {@link Disposition}.
     */
    public static Collection<Disposition> fromBits(int bits) {

        EnumSet<Disposition> set = EnumSet.noneOf(Disposition.class);
        for (Disposition d : values()) {
            if ((bits & d.bit) != 0) {
                set.add(d);
            }
        }
        return set;
    }

    /**
     * Retrieve the bit value of the provided collection of {@link Disposition}.
     *
     * @param set
     *         The collection of {@link Disposition}
     *
     * @return The bit value.
     */
    public static int toBits(Collection<Disposition> set) {

        int bits = 0;
        for (Disposition d : set) {
            bits |= d.bit;
        }
        return bits;
    }
}
