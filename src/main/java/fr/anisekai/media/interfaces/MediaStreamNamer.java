package fr.anisekai.media.interfaces;

import fr.anisekai.media.MediaStream;
import fr.anisekai.media.bin.wrapper.commands.ConvertCommandBuilder;
import fr.anisekai.media.bin.wrapper.tasks.ConvertTask;
import fr.anisekai.media.enums.Codec;

/**
 * Interface used to name a {@link MediaStream} when used in {@link ConvertTask} in split mode, most often used within a
 * {@link MediaStreamMapper}.
 */
public interface MediaStreamNamer {

    /**
     * Default {@link MediaStreamNamer} when none is provided to {@link ConvertCommandBuilder}.
     */
    MediaStreamNamer DEFAULT = (stream, codec) -> {
        Codec effectiveCodec = codec.isCopyCodec() ? stream.getCodec() : codec;
        return String.format("%s.%s", stream.getId(), effectiveCodec.getExtension());
    };

    /**
     * Retrieve the name of the {@link MediaStream} using the provided {@link Codec}.
     *
     * @param stream
     *         The {@link MediaStream} to name.
     * @param codec
     *         The {@link Codec} used to convert the {@link MediaStream}.
     *
     * @return The name of the {@link MediaStream}.
     */
    String name(MediaStream stream, Codec codec);

}
