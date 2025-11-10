package fr.anisekai.media.bin;

import fr.anisekai.media.MediaFile;
import fr.anisekai.media.MediaMeta;
import fr.anisekai.media.bin.wrapper.FFMpegCommand;
import fr.anisekai.media.bin.wrapper.commands.CombineCommandBuilder;
import fr.anisekai.media.bin.wrapper.commands.ConvertCommandBuilder;
import fr.anisekai.media.bin.wrapper.commands.MpdCommandBuilder;
import fr.anisekai.media.bin.wrapper.commands.ProbeCommandBuilder;

import java.nio.file.Path;

/**
 * Utility class providing static methods to interact with ffmpeg and ffprobe binaries.
 * <p>
 * Supports probing media files for stream information, extracting streams into separate files, and combining multiple
 * media streams into a single output file.
 */
public final class FFMpeg {

    private FFMpeg() {}

    /**
     * Create a {@link ProbeCommandBuilder} to configure a {@link FFMpegCommand} allowing to probe a {@link Path}.
     *
     * @param input
     *         The {@link Path} pointing to the file to probe.
     *
     * @return A {@link ProbeCommandBuilder}.
     */
    public static ProbeCommandBuilder probe(Path input) {

        return new ProbeCommandBuilder(input);
    }

    /**
     * Create a {@link ConvertCommandBuilder} to configure a {@link FFMpegCommand} allowing to convert a
     * {@link MediaFile}.
     *
     * @param input
     *         The {@link MediaFile} to convert
     *
     * @return A {@link ConvertCommandBuilder}.
     */
    public static ConvertCommandBuilder convert(MediaFile input) {

        return new ConvertCommandBuilder(input);
    }

    /**
     * Create a {@link CombineCommandBuilder} to configure a {@link FFMpegCommand} allowing to combine multiple
     * {@link MediaFile}.
     *
     * @param input
     *         The first {@link MediaFile} to combine
     *
     * @return A {@link CombineCommandBuilder}.
     */
    public static CombineCommandBuilder combine(MediaMeta input) {

        return new CombineCommandBuilder(input);
    }

    /**
     * Create a {@link MpdCommandBuilder} to configure a {@link FFMpegCommand} allowing to generate a meta file and
     * chunks from a {@link MediaFile}.
     *
     * @param input
     *         The {@link MediaFile} for which the meta and chunks will be generated.
     *
     * @return A {@link MpdCommandBuilder}.
     */
    public static MpdCommandBuilder mdp(MediaFile input) {

        return new MpdCommandBuilder(input);
    }

}
