package fr.anisekai.media.interfaces;

import fr.anisekai.media.MediaStream;
import fr.anisekai.media.bin.Binary;
import fr.anisekai.media.enums.Codec;
import fr.anisekai.media.enums.CodecType;
import fr.anisekai.media.enums.Disposition;

import java.util.function.BiPredicate;

/**
 * Functional interface representing a mapping operation applied when a {@link MediaStream} is added to a
 * {@link Binary}.
 * <p>
 * Implementations are typically used to inject additional arguments or metadata based on the stream and its codec.
 */
public interface MediaStreamMapper {

    /**
     * A default {@link MediaStreamMapper} implementation that adds standard ffmpeg mapping and codec selection
     * arguments to the given {@link Binary}, with additional parameters for video streams.
     * <p>
     * Specifically, it:
     * <ul>
     *   <li>Maps the input stream by its ID.</li>
     *   <li>Sets the codec using the library name corresponding to the provided {@link Codec}.</li>
     *   <li>If the codec type is {@link CodecType#VIDEO}, adds video-specific arguments:
     *       <ul>
     *           <li>Sets the constant rate factor (CRF) to 25 for quality control.</li>
     *           <li>Forces the pixel format to yuv420p.</li>
     *       </ul>
     *   </li>
     *   <li>Skips processing streams with the disposition {@link Disposition#ATTACHED_PIC}.</li>
     * </ul>
     */
    MediaStreamMapper DEFAULT = ((MediaStreamMapper) (binary, stream, codec) -> {
        binary.addArguments("-map", "0:%s".formatted(stream.getId()));
        binary.addArguments("-c:%s".formatted(codec.getType().getChar()), codec.getLibName());

        if (codec.getType() == CodecType.VIDEO && !codec.isCopyCodec()) {
            binary.addArguments("-crf", 25);
            binary.addArguments("-vf", "format=yuv420p");
        }
    }).onlyIf((stream, codec) -> !stream.getDispositions().contains(Disposition.ATTACHED_PIC));

    /**
     * Applies mapping logic to the given {@link MediaStream} and {@link Codec}, inserting any required arguments or
     * metadata into the target {@link Binary}.
     *
     * @param binary
     *         The target {@link Binary} to populate with arguments or metadata.
     * @param stream
     *         The {@link MediaStream} being mapped.
     * @param codec
     *         The {@link Codec} associated with the stream.
     */
    void map(Binary binary, MediaStream stream, Codec codec);

    /**
     * Chains this mapper with another, applying both mappers in sequence. The current mapper is invoked first, followed
     * by the {@code other}.
     *
     * @param other
     *         The {@link MediaStreamMapper} to invoke after this one.
     *
     * @return A composed {@link MediaStreamMapper} that applies both mappings in order.
     */
    default MediaStreamMapper then(MediaStreamMapper other) {

        return (binary, stream, codec) -> {
            this.map(binary, stream, codec);
            other.map(binary, stream, codec);
        };
    }

    /**
     * Returns a conditional {@link MediaStreamMapper} that only applies this mapper if the given predicate evaluates to
     * {@code true}.
     *
     * @param predicate
     *         A {@link BiPredicate} testing the {@link MediaStream} and {@link Codec}.
     *
     * @return A conditional {@link MediaStreamMapper}.
     */
    default MediaStreamMapper onlyIf(BiPredicate<MediaStream, Codec> predicate) {

        return (binary, stream, codec) -> {
            if (predicate.test(stream, codec)) {
                this.map(binary, stream, codec);
            }
        };
    }

}
